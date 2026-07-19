import * as Utils from '/util/utils.js';

// Centralized reference states for DOM Nodes to prevent scope leaking duplication
let queryInput, searchBtn, queryDropdown, displayBox;
let updateForm, inputPostcode, inputLatitude, inputLongitude, btnSubmitUpdate, statusBanner;

function cacheDOMElements() {
    // Left panel: Query components
    queryInput = document.getElementById('queryPostcode');
    searchBtn = document.getElementById('querySearchBtn');
    queryDropdown = document.getElementById('postcodeSuggestions');
    displayBox = document.getElementById('queryResultDisplay');

    // Right panel: Form input components
    updateForm = document.getElementById('updatePostcodeForm');
    inputPostcode = document.getElementById('inputPostcode');
    inputLatitude = document.getElementById('inputLatitude');
    inputLongitude = document.getElementById('inputLongitude');
    btnSubmitUpdate = document.getElementById('btnSubmitUpdate');
    statusBanner = document.getElementById('updateStatusBanner');
}

function inputValidations(postcode, latitude, longitude, messageBanner) {
    const ukPostcodeRegex = /^[A-Z]{1,2}[0-9][A-Z0-9]? ?[0-9][A-Z]{2}$/;

    if (!ukPostcodeRegex.test(postcode)) {
        messageBanner.className = "message-banner error";
        messageBanner.textContent = "❌ Invalid UK Postcode format. Example: SW1A 1AA";
        return false;
    }

    if (isNaN(latitude) || latitude < -90 || latitude > 90) {
        messageBanner.className = "message-banner error";
        messageBanner.textContent = "❌ Latitude must be a valid number between -90 and 90.";
        return false;
    }

    if (isNaN(longitude) || longitude < -180 || longitude > 180) {
        messageBanner.className = "message-banner error";
        messageBanner.textContent = "❌ Longitude must be a valid number between -180 and 180.";
        return false;
    }
    return true;
}

function validateQueryInput() {
    if (searchBtn && queryInput) {
        searchBtn.disabled = queryInput.value.trim().length === 0;
    }
}

function validateUpdateFormInputs() {
    if (btnSubmitUpdate && inputPostcode && inputLatitude && inputLongitude) {
        btnSubmitUpdate.disabled = inputPostcode.value.trim().length === 0 ||
                                   inputLatitude.value.trim().length === 0 ||
                                   inputLongitude.value.trim().length === 0;
    }
}

function initEventListeners() {
    // Utilize shared modules from utils.js
    Utils.setupAutocomplete('queryPostcode', 'postcodeSuggestions');
    Utils.initSidebarToggle();

    // Event listeners tracking dynamic validation states
    queryInput.addEventListener('input', validateQueryInput);
    inputPostcode.addEventListener('input', validateUpdateFormInputs);
    inputLatitude.addEventListener('input', validateUpdateFormInputs);
    inputLongitude.addEventListener('input', validateUpdateFormInputs);

    // Close autocompletes if clicking outside components
    document.addEventListener('click', (e) => {
        if (e.target.id !== 'queryPostcode' && queryDropdown && !queryDropdown.contains(e.target)) {
            queryDropdown.style.display = 'none';
        }
    });

    // --- 1. GET Query Handler ---
    searchBtn.addEventListener('click', async () => {
        const targetCode = queryInput.value.trim().toUpperCase();
        if (!targetCode) return;

        displayBox.className = "query-result-box";
        displayBox.innerHTML = "Searching records...";

        try {
            const response = await fetch(`/api/postcodes/searchQuery?postcode=${encodeURIComponent(targetCode)}`, {
                method: 'GET',
                headers: { 'Content-Type': 'application/json' }
            });

            if (response.ok) {
                const data = await response.json();
                const postcodeDetail = data.result;

                displayBox.innerHTML = `
                    <div class="result-detail-item"><strong>Postal Code:</strong> <span>${postcodeDetail.postcode}</span></div>
                    <div class="result-detail-item"><strong>Latitude:</strong> <span>${postcodeDetail.latitude}</span></div>
                    <div class="result-detail-item"><strong>Longitude:</strong> <span>${postcodeDetail.longitude}</span></div>
                `;
            } else {
                displayBox.className = "query-result-box empty";
                displayBox.innerHTML = `No record found for "${targetCode}".`;
            }
        } catch (error) {
            displayBox.className = "query-result-box empty";
            displayBox.innerHTML = `No record found for "${targetCode}".`;
        }
    });

    // --- 2. POST / PUT Update Handler ---
    updateForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        const postcodeVal = inputPostcode.value.trim().toUpperCase();
        const latVal = parseFloat(inputLatitude.value);
        const lngVal = parseFloat(inputLongitude.value);

        if (!inputValidations(postcodeVal, latVal, lngVal, statusBanner)) {
            return;
        }

        const payload = {
            postcode: postcodeVal,
            latitude: latVal,
            longitude: lngVal
        };

        statusBanner.className = "message-banner";
        statusBanner.textContent = "Saving changes...";
        statusBanner.style.display = "block";

        try {
            const response = await fetch('/api/postcodes/insertOrUpdate', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const message = await response.text();

            if (response.ok) {
                statusBanner.className = "message-banner success";
                statusBanner.textContent = message;
                updateForm.reset();
                validateUpdateFormInputs(); // Recalculate component button states after explicit form resets
            } else {
                statusBanner.className = "message-banner error";
                statusBanner.textContent = message;
            }
        } catch (err) {
            statusBanner.className = "message-banner error";
            statusBanner.textContent = "Unable to use create or update API. Network error.";
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
    validateQueryInput();
    validateUpdateFormInputs();
    Utils.fetchAndSetUserProfile(null);
})();

window.addEventListener('dashboardEvents', (e) => {
    if (e.detail && e.detail.username) {
        Utils.fetchAndSetUserProfile(e.detail.username);
    }
});