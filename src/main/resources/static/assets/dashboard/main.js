import * as Utils from '/assets/util/utils.js';

// Centralized reference states for DOM Nodes to prevent scope leaking duplication
let fromInput, toInput, submitBtn, fromDropdown, toDropdown, hintContainer;

function cacheDOMElements() {
    fromInput = document.getElementById('postcodeFrom');
    toInput = document.getElementById('postcodeTo');
    submitBtn = document.getElementById('searchSubmitBtn');
    fromDropdown = document.getElementById('postcodeFromSuggestions');
    toDropdown = document.getElementById('postcodeToSuggestions');
    hintContainer = document.getElementById('suggestionHint');
}

function updateHintVisibility() {
    if (!hintContainer || !fromInput || !toInput) return;
    const fromLen = fromInput.value.trim().length;
    const toLen = toInput.value.trim().length;

    if ((fromLen > 0 && fromLen < 2) || (toLen > 0 && toLen < 2)) {
        hintContainer.style.display = 'flex';
    } else {
        hintContainer.style.display = 'none';
    }
}

function validateFormInputs() {
    if (!fromInput || !toInput || !submitBtn) return;
    const hasFromValue = fromInput.value.trim().length > 0;
    const hasToValue = toInput.value.trim().length > 0;
    submitBtn.disabled = !(hasFromValue && hasToValue);
}

function checkPostalElements() {
    const container1 = fromInput.parentElement;
    const container2 = toInput.parentElement;

    Utils.hideElement('searchResult', true);
    container1.classList.remove('input-field-error');
    container2.classList.remove('input-field-error');

    const p1 = fromInput.value.trim().toLowerCase();
    const p2 = toInput.value.trim().toLowerCase();

    if (p1 === p2) {
        Utils.showError('searchResult', 'Starting postcode and destination postcode cannot be the same.');
        container1.classList.add('input-field-error');
        container2.classList.add('input-field-error');
        fromInput.focus();
        return false;
    }
    return true;
}

function initEventListeners() {
    // Shared module integrations brought from Utils framework
    Utils.setupAutocomplete('postcodeFrom', 'postcodeFromSuggestions');
    Utils.setupAutocomplete('postcodeTo', 'postcodeToSuggestions');
    Utils.initSidebarToggle();

    // Context changes dynamic tracking configurations
    const trackInputChanges = () => {
        validateFormInputs();
        updateHintVisibility();
    };

    fromInput.addEventListener('input', trackInputChanges);
    toInput.addEventListener('input', trackInputChanges);

    // Automatically remove red border decoration when typing updates fix validation
    document.addEventListener('input', (event) => {
        const container = event.target.parentElement;
        if (container && container.classList.contains('input-field-error') && event.target.value.trim() !== "") {
            container.classList.remove('input-field-error');
        }
    });

    // Close autocompletes if clicking layout body sections outwards
    document.addEventListener('click', (e) => {
        if (e.target.id !== 'postcodeFrom' && e.target !== fromDropdown && fromDropdown) {
            fromDropdown.style.display = 'none';
        }
        if (e.target.id !== 'postcodeTo' && e.target !== toDropdown && toDropdown) {
            toDropdown.style.display = 'none';
        }
    });

    // Swap Field Handler
    document.getElementById('swapBtn')?.addEventListener('click', () => {
        const temp = fromInput.value;
        fromInput.value = toInput.value;
        toInput.value = temp;
        trackInputChanges(); // Recalculate component button validations post-swap
    });

    // Submission Context Operations Routing Engine
    document.getElementById('postcodeForm')?.addEventListener('submit', async (formEvent) => {
        formEvent.preventDefault();
        if (!checkPostalElements()) return;

        const jsonOutput = document.getElementById('jsonOutput');
        if (jsonOutput) jsonOutput.textContent = "";

        try {
            const response = await fetch(`/api/postcodes/searchRoute?postCodeFrom=${encodeURIComponent(fromInput.value.trim())}&postCodeTo=${encodeURIComponent(toInput.value.trim())}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });

            if (response.ok) {
                const data = await response.json();
                const route = data.result;

                if (route) {
                    document.getElementById('distanceValue').textContent = route.distance ? route.distance.toFixed(2) : '0.00';
                    document.getElementById('lblFromPostcode').textContent = route.fromPostcode;
                    document.getElementById('lblToPostcode').textContent = route.toPostcode;
                    document.getElementById('fromLatitude').textContent = `Latitude: ${route.fromLat}`;
                    document.getElementById('fromLongitude').textContent = `Longitude: ${route.fromLng}`;
                    document.getElementById('toLatitude').textContent = `Latitude: ${route.toLat}`;
                    document.getElementById('toLongitude').textContent = `Longitude: ${route.toLng}`;
                    document.getElementById('statusMessage').textContent = `Calculated straight-line distance successfully.`;
                }
                if (jsonOutput) jsonOutput.textContent = JSON.stringify(data, null, 2);
            } else {
                const errMsg = await response.text();
                Utils.showError('searchResult', errMsg);
            }
        } catch (err) {
            console.error("API Error context stack trace log:", err);
            Utils.showError('searchResult', 'Unable to query API. Network error.');
        }
    });

    // Unified Clean Sign Out Handler
    document.getElementById('logoutBtn')?.addEventListener('click', async (logoutEvent) => {
        logoutEvent.preventDefault();
        try {
            await fetch('/api/logout', { method: 'POST' });
        } catch (err) {
            console.error("Logout request failed:", err);
        }
        window.location.assign('/tdm/home');
    });
}

// RUN IMMEDIATELY ON MODULE LOAD
(function init() {
    cacheDOMElements();
    initEventListeners();
    validateFormInputs();
    Utils.fetchAndSetUserProfile(null);
})();

window.addEventListener('dashboardEvents', (e) => {
    if (e.detail && e.detail.username) {
        Utils.fetchAndSetUserProfile(e.detail.username);
    }
});