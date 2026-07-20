// To reload the script content of a JS file
export function reloadScript(scriptPath, eventName, payload = {}) {
    // Remove the inert template script tag if present
    document.querySelector(`script[src*="${scriptPath}"]`).remove();

    // Create and append an active script tag
    const script = document.createElement('script');
    script.type = 'module';
    script.src = scriptPath;

    script.onload = () => {
        window.dispatchEvent(new CustomEvent(eventName, {
            detail: payload
        }));
    };

    document.body.appendChild(script);
}

// To display errors
export const showError = (elementId, message) => {
    const el = document.getElementById(elementId);
    if (el) {
        el.textContent = message;
        el.classList.remove('hidden');
    }
};

// To control element visibility
export const hideElement = (idOrElement, isHidden) => {
    const el = typeof idOrElement === 'string' ? document.getElementById(idOrElement) : idOrElement;
    if (el) el.classList.toggle('hidden', isHidden);
};

// To clear input fields
export const clearFields = (containerId) => {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.querySelectorAll('input[type="text"], input[type="email"], input[type="password"]')
        .forEach(input => input.value = '');
};

// To load username of the current profile
export async function fetchAndSetUserProfile(username) {
    const userProfileSpan = document.querySelector('.user-profile');
    if (!userProfileSpan) return;

    if (username) {
        userProfileSpan.textContent = `Welcome, ${username}`;
        return;
    }

    try {
        const response = await fetch('/api/postcodes/getCurrentUser');
        if (response.ok) {
            const userData = await response.json();
            if (userData && userData.username) {
                userProfileSpan.textContent = `Welcome, ${userData.username}`;
            }
        }
    } catch (err) {
        console.error("Failed to fetch user profile:", err);
    }
}

// Reusable Autocomplete Setup
export function setupAutocomplete(inputId, dropdownId, suggestionLimit = 8) {
    const input = document.getElementById(inputId);
    const dropdown = document.getElementById(dropdownId);
    let debounceTimeout;
    let isSelecting = false;

    if (!input || !dropdown) return;

    input.addEventListener('input', () => {
        if (isSelecting) {
            isSelecting = false;
            return;
        }

        clearTimeout(debounceTimeout);
        const query = input.value.trim();

        if (query.length < 2) {
            dropdown.innerHTML = '';
            dropdown.style.display = 'none';
            return;
        }

        debounceTimeout = setTimeout(async () => {
            try {
                const response = await fetch(`/api/postcodes/suggest?query=${encodeURIComponent(query)}&nums=${encodeURIComponent(suggestionLimit)}`, {
                    method: 'GET',
                    headers: { 'Content-Type': 'application/json' }
                });
                const data = await response.json();

                if (response.ok && data.result) {
                    dropdown.innerHTML = '';

                    // --- FIX: Check if the user typed the exact match of the first result ---
                    if (data.result.length === 0 || (data.result.length === 1 && data.result[0].toUpperCase() === query.toUpperCase())) {
                        dropdown.style.display = 'none';
                        return;
                    }

                    data.result.forEach(item => {
                        const div = document.createElement('div');
                        div.className = 'suggestion-item';
                        div.textContent = item;
                        div.addEventListener('click', (e) => {
                            e.stopPropagation();

                            isSelecting = true;

                            input.value = item;
                            dropdown.innerHTML = '';
                            dropdown.style.display = 'none';
                            input.parentElement.classList.remove('input-field-error');

                            input.dispatchEvent(new Event('input', { bubbles: true }));

                            input.blur();
                        });
                        dropdown.appendChild(div);
                    });

                    // Double check after building items (covers edge case where first option matches input out of multiple results)
                    if (data.result[0].toUpperCase() === query.toUpperCase() && data.result.length === 1) {
                        dropdown.style.display = 'none';
                    } else {
                        dropdown.style.display = 'block';
                    }
                } else {
                    dropdown.innerHTML = '';
                    dropdown.style.display = 'none';
                }
            } catch (error) {
                console.error('Error fetching postcodes:', error);
                dropdown.innerHTML = '';
                dropdown.style.display = 'none';
            }
        }, 300);
    });

    input.addEventListener('focus', () => {
        // Prevent showing the dropdown if the text in the input box matches the only option available
        const currentQuery = input.value.trim().toUpperCase();
        if (dropdown.children.length === 1 && dropdown.children[0].textContent.toUpperCase() === currentQuery) {
            dropdown.style.display = 'none';
            return;
        }

        if (input.value.trim().length >= 2 && dropdown.children.length > 0) {
            dropdown.style.display = 'block';
        }
    });
}

// Shared Component: Standard UI Sidebar Frame Controller Actions
export function initSidebarToggle(sidebarId = 'sidebar', toggleBtnId = 'sidebarToggle') {
    const sidebar = document.getElementById(sidebarId);
    const toggleBtn = document.getElementById(toggleBtnId);
    if (toggleBtn && sidebar) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
        });
    }
}