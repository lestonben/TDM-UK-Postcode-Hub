import * as Utils from '/js/utils.js';

function initEventListeners() {
    Utils.initSidebarToggle();
    Utils.initLogOutFunction();
}

// RUN IMMEDIATELY ON MODULE LOAD
(function init() {
    initEventListeners();
    Utils.fetchAndSetUserProfile(null);
})();

window.addEventListener('dashboardEvents', (e) => {
    if (e.detail && e.detail.username) {
        Utils.fetchAndSetUserProfile(e.detail.username);
    }
});