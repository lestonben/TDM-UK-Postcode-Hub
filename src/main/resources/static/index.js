import * as Utils from '/assets/util/utils.js';

// --- Event Handlers ---
document.getElementById('loginForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const usernameEmail = document.getElementById('userId').value;
    const password = document.getElementById('userPass').value;

    if (!loginInputValidations(usernameEmail, password)) {
        return;
    }

    const payload = {
            usernameEmail: usernameEmail,
            password: password
    };

    try {
        const response = await postAPI('/api/login', payload);

        if (!response.ok) {
            const errorMsg = await response.text();
            Utils.showError('loginError', errorMsg || "Invalid user ID or password.");
            return;
        }

        // Fetch and load dashboard view
        const pageResponse = await await fetch('/tdm/dashboard/main', { method: 'GET' });

        if (!pageResponse.ok) {
            Utils.showError('loginError', `Access denied to dashboard (${pageResponse.status}).`);
            return;
        }

        document.documentElement.innerHTML = await pageResponse.text();
        window.history.pushState({}, '', '/tdm/dashboard/main');

        Utils.reloadScript('assets/dashboard/main.js', 'dashboardEvents', { username: usernameEmail });

    } catch (err) {
        Utils.showError('loginError', "Unable to connect to the server. Please check your network.");
    }
});

document.getElementById('registerForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const username = document.getElementById('usernameInput').value;
    const email = document.getElementById('emailInput').value;
    const password = document.getElementById('passInput').value;

    if (!registerInputValidations(username, email, password)) {
        return;
    }

    const payload = {
        username: username,
        email: email,
        password: password
    };

    try {
        const response = await postAPI('/api/register', payload);
        if (response.ok) {
            window.location.href = '/tdm/home';
        } else {
            const errorMessage = await response.text();
            Utils.showError('regError', errorMessage || "Please retry again later.");
        }
    } catch (err) {
        Utils.showError('regError', "Unable to connect to the server. Please check your network.");
    }
});

// --- Input Field Validation ---
function loginInputValidations(usernameEmail, password) {
    if (!usernameEmail || usernameEmail.trim() === "") {
        Utils.showError('loginError', "Please fill in your username or email.");
        return false;
    }

    if (!password || password.trim() === "") {
        Utils.showError('loginError', "Please fill in your password.");
        return false;
    }
    return true;
}

function registerInputValidations(username, email, password) {
    // Check if username is empty
    if (!username || username.trim() === "") {
        Utils.showError('regError', "Please fill in your username.");
        return false;
    }

    // Check if email is empty
    if (!email || email.trim() === "") {
        Utils.showError('regError', "Please fill in your email.");
        return false;
    }

    // Check if password is empty
    if (!password || password.trim() === "") {
        Utils.showError('regError', "Please fill in your password.");
        return false;
    }
    return true;
}

// --- View Toggling & Navigation ---
function toggleAuthViews(target) {
    const isRegister = (target === 'register');

    Utils.hideElement('loginForm', isRegister);
    Utils.hideElement(document.querySelector('#loginSection h2'), isRegister);
    Utils.hideElement('loginBar', !isRegister);
    Utils.hideElement('signupBar', isRegister);
    Utils.hideElement('registerFormFields', !isRegister);

    if (isRegister) {
        Utils.clearFields('loginForm');
        Utils.hideElement('loginError', true);
    } else {
        Utils.clearFields('registerFormFields');
        Utils.hideElement('regError', true);
    }
}

window.toggleAuthViews = toggleAuthViews;

window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
        Utils.clearFields('loginForm');
        Utils.clearFields('registerFormFields');
        Utils.hideElement('loginError', true);
        Utils.hideElement('regError', true);
    }
});

async function postAPI(url, payload) {
    return fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    });
}