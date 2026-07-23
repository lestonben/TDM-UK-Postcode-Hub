import * as Utils from '/js/utils.js';

const REGISTER_SITE_KEY = "6LcdQ2AtAAAAABcaItYRyOvu0CqwhaGqgmbSU_Z5";

// --- Event Handlers ---
document.getElementById('loginForm').addEventListener('submit', async(e) => {
    e.preventDefault();

    Utils.promptRecaptchaModal(REGISTER_SITE_KEY, async(recaptchaToken) => {
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

            const responseMessage = await response.text();
            if (!response.ok) {
                Utils.showError('loginError', responseMessage || "Invalid user ID or password.");
                return;
            }

            // Fetch and load dashboard view (fixed double await typo here)
            const pageResponse = await fetch('/tdm/dashboard/main', {
                method: 'GET'
            });

            if (!pageResponse.ok) {
                Utils.showError('loginError', `Access denied to dashboard (${pageResponse.status}).`);
                return;
            }
            const dashboardHtml = await pageResponse.text();

            Utils.showSuccess('loginError', responseMessage || "Login successful. Auto-redirecting soon..");
            setTimeout(() => {
                document.documentElement.innerHTML = dashboardHtml;
                window.history.pushState({}, '', '/tdm/dashboard/main');

                Utils.reloadScript('/js/dashboard-main.js', 'dashboardEvents', {
                    username: usernameEmail
                });
            }, 1500);
        }
        catch (err) {
            Utils.showError('loginError', "Unable to connect to the server. Please check your network.");
        }
    });
});

document.getElementById('registerForm').addEventListener('submit', (e) => {
    e.preventDefault();

    Utils.promptRecaptchaModal(REGISTER_SITE_KEY, async (recaptchaToken) => {
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

            const responseMessage = await response.text();
            if (response.ok) {
                Utils.showSuccess('regError', responseMessage || "Registration successful!");

                setTimeout(() => {
                    window.location.href = '/tdm/home';
                }, 1500);
            } else {
                Utils.showError('regError', responseMessage || "Please retry again later.");
            }
        } catch (err) {
            Utils.showError('regError', "Unable to connect to the server. Please check your network.");
        }
    });
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
    if (!username || username.trim() === "") {
        Utils.showError('regError', "Please fill in your username.");
        return false;
    }

    if (!email || email.trim() === "") {
        Utils.showError('regError', "Please fill in your email.");
        return false;
    }

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