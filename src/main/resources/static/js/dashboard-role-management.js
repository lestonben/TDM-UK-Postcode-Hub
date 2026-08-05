import * as Utils from '/js/utils.js';

// Centralized reference states for DOM Nodes
let createRoleForm, assignRoleForm, roleNameInput, roleDescInput, selectUser, selectRole, tableFilter, roleMatrixBody, currentSearch, statusBanner, saveRolePagesDefinitionBtn, modalTitleText, modalSubmitBtn;

// Additional DOM Nodes for User Role Editing Modal
let editUserIdInput, editUsernameInput, userRolesCheckboxContainer, updateUserStatusBanner, saveUserRolesBtn;

// Pagination, edit states, and cached data
let currentRoleMappingPage = 0, totalRoleMappingPages = 1;
let isEditMode = false, currentEditingRoleId = null;
let cachedRoleMappingData = [];

// User-to-Role states and caches
let currentPage = 0, totalPages = 1;
const pageSize = 10;
let currentEditingUserId = null;
let cachedUserMatrixData = [];
let allAvailableRoles = [];

function cacheDOMElements() {
    createRoleForm = document.getElementById('createRoleForm');
    assignRoleForm = document.getElementById('assignRoleForm');
    roleNameInput = document.getElementById('roleNameInput');
    roleDescInput = document.getElementById('roleDescInput');
    selectUser = document.getElementById('selectUser');
    selectRole = document.getElementById('selectRole');
    tableFilter = document.getElementById('tableFilter');
    roleMatrixBody = document.getElementById('roleMatrixBody');
    statusBanner = document.getElementById('updateStatusBanner');
    saveRolePagesDefinitionBtn = document.getElementById('saveRolePagesDefinitionBtn');

    // User Role Modal Elements
    editUserIdInput = document.getElementById('editUserIdInput');
    editUsernameInput = document.getElementById('editUsernameInput');
    userRolesCheckboxContainer = document.getElementById('userRolesCheckboxContainer');
    updateUserStatusBanner = document.getElementById('updateUserStatusBanner');
    saveUserRolesBtn = document.getElementById('saveUserRolesBtn');

    modalTitleText = document.getElementById('modalTitleText') || document.querySelector('#roleModal .modal-title, #roleModal h3, #roleModal .font-bold');
    modalSubmitBtn = saveRolePagesDefinitionBtn;
}

/**
 * Create New Role Mapping Input Validations
 */
function validateRoleFormInputs() {
    const roleName = roleNameInput.value.trim();
    const roleDesc = roleDescInput.value.trim();
    const selectedPages = document.querySelectorAll('input.form-checkbox:checked');

    // Enable button ONLY if all criteria are met
    if (roleName !== "" && roleDesc !== "" && selectedPages.length > 0) {
        saveRolePagesDefinitionBtn.disabled = false;
    }
    else {
        saveRolePagesDefinitionBtn.disabled = true;
    }
}

/**
 * Reusable helper to generate standard role badges with consistent dynamic colors.
 */
const roleBadgeColorMap = new Map();

function getDynamicBadgeColor(roleName) {
    let hash = 0;
    for (let i=0; i<roleName.length; i++) {
        hash = roleName.charCodeAt(i) + ((hash << 5) - hash);
    }

    const hue = Math.abs(hash) % 360;
    return `background-color: hsl(${hue}, 80%, 92%); color: hsl(${hue}, 70%, 28%); border: 1px solid hsl(${hue}, 70%, 82%);`;
}

function createRoleBadge(roleName, extraStyles = '') {
    if (!roleBadgeColorMap.has(roleName)) {
        roleBadgeColorMap.set(roleName, getDynamicBadgeColor(roleName));
    }
    const roleBadgeStyle = roleBadgeColorMap.get(roleName);
    const combineStyle = extraStyles ? `${extraStyles} ${roleBadgeStyle}` : roleBadgeStyle;

    return `<span class="role-badge" style="${combineStyle}">${roleName}</span>`;
}

/**
 * Reusable helper to update pagination controls dynamically for any table.
 */
function updatePaginationUI(currPage, totPages, infoDisplayId, prevBtnId, nextBtnId, fetchCallback) {
    const pageInfoDisplay = document.getElementById(infoDisplayId);
    const prevPageBtn = document.getElementById(prevBtnId);
    const nextPageBtn = document.getElementById(nextBtnId);

    if (pageInfoDisplay) pageInfoDisplay.textContent = `Showing page ${currPage + 1} of ${totPages}`;
    if (prevPageBtn) {
        prevPageBtn.disabled = currPage <= 0;
        prevPageBtn.onclick = () => currPage > 0 && fetchCallback(currPage - 1);
    }
    if (nextPageBtn) {
        nextPageBtn.disabled = currPage >= totPages - 1;
        nextPageBtn.onclick = () => currPage < totPages - 1 && fetchCallback(currPage + 1);
    }
}

/**
 * Reusable helper for generic collapse toggle sections.
 */
function initCollapseToggle(btnId, sectionId, iconId) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.addEventListener('click', () => {
        const section = document.getElementById(sectionId);
        const icon = document.getElementById(iconId);
        const isHidden = section.style.display === 'none';
        section.style.display = isHidden ? 'block' : 'none';
        icon.className = isHidden ? 'fa-solid fa-chevron-down' : 'fa-solid fa-chevron-up';
    });
}

/**
 * Reusable helper to render page pills or tag lists inside table cells.
 */
function renderTagsList(pages = []) {
    if (!pages.length) return `<span style="color: var(--text-muted); font-size: 12px;">No pages mapped</span>`;

    const createTag = (text, title = '') => `
        <span style="display: inline-block; background: #e0f2fe; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; margin-right: 4px; margin-bottom: 2px; color: #0369a1;" ${title ? `title="${title}"` : ''}>${text}</span>
    `;

    if (pages.length <= 2) {
        return pages.map(p => createTag(p)).join('');
    }

    const visibleTags = pages.slice(0, 2).map(p => createTag(p)).join('');
    const moreIndicator = `<span style="display: inline-block; background: #f1f5f9; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: 500; margin-right: 4px; margin-bottom: 2px; color: var(--text-muted);" title="${pages.slice(2).join(', ')}"> +${pages.length - 2} more </span>`;

    return visibleTags + moreIndicator;
}

function toggleModal(show, editMode = false) {
    const modal = document.getElementById('roleModal');
    if (modal) modal.style.display = show ? 'flex' : 'none';

    if (show) {
        isEditMode = editMode;
        if (modalTitleText) {
            modalTitleText.textContent = editMode ? 'Update Role Mapping' : 'Create New Role Mapping';
        }
        if (modalSubmitBtn) {
            modalSubmitBtn.textContent = editMode ? 'Update Role Definition' : 'Save Role Definition';
        }
    } else {
        isEditMode = false;
        currentEditingRoleId = null;
        if (roleNameInput) roleNameInput.disabled = false;
        saveRolePagesDefinitionBtn.disabled = false;
    }
}

function toggleUserRoleModal(show) {
    const modal = document.getElementById('userRoleModal');
    if (modal) modal.style.display = show ? 'flex' : 'none';
    if (!show) {
        currentEditingUserId = null;
        if (updateUserStatusBanner) updateUserStatusBanner.className = "hidden";
    }
    saveUserRolesBtn.disabled = false;
}

async function fetchAllRoles() {
    try {
        allAvailableRoles = [];

        const response = await fetch('/api/roles/getAllRolesList');
        if (response.ok) {
            const data = await response.json();
            // Extract the list from the 'result' field returned by your backend
            allAvailableRoles = data.result || data || [];
        }
    } catch (err) {
        console.error("Failed to fetch available roles:", err);
    }
}

function initEventListeners() {
    Utils.initSidebarToggle();
    Utils.initLogOutFunction();

    roleNameInput.addEventListener('input', validateRoleFormInputs);
    roleDescInput.addEventListener('input', validateRoleFormInputs);

    // Listen to changes on all page access checkboxes
    document.querySelectorAll('input.form-checkbox').forEach(checkbox => {
        checkbox.addEventListener('change', validateRoleFormInputs);
    });

    // Live filter search for Role Assignment Matrix table
    if (tableFilter && roleMatrixBody) {
        tableFilter.addEventListener('input', (e) => {
            const query = e.target.value.toLowerCase();
            roleMatrixBody.querySelectorAll('tr').forEach(row => {
                row.style.display = row.textContent.toLowerCase().includes(query) ? '' : 'none';
            });
        });
    }

    // Role-to-Pages Live Search Input & Enter Key Trigger
    const roleMappingFilter = document.getElementById('roleMappingFilter');
    if (roleMappingFilter) {
        const triggerSearch = () => loadRolesPages(0);
        roleMappingFilter.addEventListener('input', triggerSearch);
        roleMappingFilter.addEventListener('keypress', (e) => e.key === 'Enter' && triggerSearch());
    }

    // Initialize Collapses
    initCollapseToggle('roleMappingCollapseBtn', 'roleMappingCollapseSection', 'roleMappingCollapseIcon');
    initCollapseToggle('collapseBtn', 'tableCollapseSection', 'collapseIcon');

    // Modal Control Handlers
    const modal = document.getElementById('roleModal');
    const openRoleModalBtn = document.getElementById('openRoleModalBtn');
    const closeRoleModalBtn = document.getElementById('closeRoleModalBtn');
    const cancelRoleModalBtn = document.getElementById('cancelRoleModalBtn');

    if (openRoleModalBtn) {
        openRoleModalBtn.addEventListener('click', () => {
            validateRoleFormInputs();

            currentEditingRoleId = null;
            if (roleNameInput) { roleNameInput.value = ''; roleNameInput.disabled = false; }
            if (roleDescInput) roleDescInput.value = '';
            document.querySelectorAll('.form-checkbox').forEach(cb => cb.checked = false);
            if (statusBanner) statusBanner.className = "hidden";
            toggleModal(true, false);
        });
    }

    [closeRoleModalBtn, cancelRoleModalBtn].forEach(btn => {
        if (btn) btn.addEventListener('click', () => toggleModal(false, false));
    });

    window.addEventListener('click', (e) => {
        if (e.target === modal) toggleModal(false, false);
    });

    // User Role Modal Close Handlers
    const userRoleModal = document.getElementById('userRoleModal');
    const closeUserRoleModalBtn = document.getElementById('closeUserRoleModalBtn');
    if (closeUserRoleModalBtn) {
        closeUserRoleModalBtn.addEventListener('click', () => toggleUserRoleModal(false));
    }
    window.addEventListener('click', (e) => {
        if (e.target === userRoleModal) toggleUserRoleModal(false);
    });

    // Search Matrix triggers
    const searchBtn = document.getElementById('searchBtn');
    if (searchBtn) searchBtn.addEventListener('click', () => loadUsersRoles(0));
    if (tableFilter) {
        tableFilter.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') loadUsersRoles(0);
        });
    }

    // Save or Update Role Definition Form Submission
    if (saveRolePagesDefinitionBtn) {
        saveRolePagesDefinitionBtn.addEventListener('click', async (event) => {
            event.preventDefault();

            // Include ID in the payload when in edit mode
            const payload = {
                ...(isEditMode && currentEditingRoleId ? { roleId: currentEditingRoleId } : {}),
                roleName: roleNameInput ? roleNameInput.value : '',
                roleDescription: roleDescInput ? roleDescInput.value : '',
                accessPagesUrl: Array.from(document.querySelectorAll('.form-checkbox:checked')).map(cb => cb.value)
            };

            const endpoint = isEditMode ? '/api/roles/updateRolePages' : '/api/roles/createRolePages';

            try {
                const response = await fetch(endpoint, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': Utils.getCsrfToken()
                    },
                    body: JSON.stringify(payload)
                });

                saveRolePagesDefinitionBtn.disabled = true;
                const responseMessage = await response.text();
                statusBanner.className = `message-banner ${response.ok ? 'success' : 'error'}`;
                statusBanner.textContent = responseMessage;

                if (response.ok) {
                    setTimeout(() => {
                        statusBanner.className = "hidden";
                        if (roleNameInput) roleNameInput.value = '';
                        if (roleDescInput) roleDescInput.value = '';
                        document.querySelectorAll('.form-checkbox').forEach(cb => cb.checked = false);
                        toggleModal(false, false);
                        loadRolesPages(currentRoleMappingPage);
                        fetchAllRoles();
                    }, 3000);
                }
            } catch (error) {
                statusBanner.className = "message-banner error";
                statusBanner.textContent = "Unable to use create or update API. Network error.";
            }
        });
    }

    // Save User Roles Changes Submission
    if (saveUserRolesBtn) {
        saveUserRolesBtn.addEventListener('click', async (event) => {
            event.preventDefault();

            const selectedRoles = Array.from(document.querySelectorAll('input[name="userRoleCheckbox"]:checked')).map(cb => cb.value);

            const payload = {
                userId: currentEditingUserId,
                roles: selectedRoles
            };

            try {
                const response = await fetch('/api/roles/updateUserRoles', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-XSRF-TOKEN': Utils.getCsrfToken()
                    },
                    body: JSON.stringify(payload)
                });

                saveUserRolesBtn.disabled = true;
                const responseMessage = await response.text();
                updateUserStatusBanner.className = `message-banner ${response.ok ? 'success' : 'error'}`;
                updateUserStatusBanner.textContent = responseMessage;

                if (response.ok) {
                    setTimeout(() => {
                        toggleUserRoleModal(false);
                        loadUsersRoles(currentPage);
                    }, 3000);
                }
            } catch (error) {
                updateUserStatusBanner.className = "message-banner error";
                updateUserStatusBanner.textContent = "Unable to update user roles. Network error.";
            }
        });
    }
}

// Populate modal directly from cached table data and switch to edit configuration mode
window.editRoleMapping = function(roleId) {
    currentEditingRoleId = roleId;

    const item = cachedRoleMappingData.find(r => r.id === roleId);
    if (!item) {
        console.error("Role mapping record not found in cache.");
        return;
    }

    if (roleNameInput) {
        roleNameInput.value = item.roleName || '';
        roleNameInput.disabled = true;
    }
    if (roleDescInput) {
        roleDescInput.value = item.roleDesc || item.roleDescription || '';
    }

    const allowedPages = item.pagesCanAccess || item.accessPagesUrl || item.pages || [];

    document.querySelectorAll('.form-checkbox').forEach(cb => {
        const labelContainer = cb.closest('label') || cb.parentElement;
        const fullText = labelContainer ? labelContainer.textContent : '';

        const urlValue = cb.value ? cb.value.trim().toLowerCase() : '';
        const cleanText = fullText.replace(cb.value, '').trim().toLowerCase();

        cb.checked = allowedPages.some(page => {
            const target = String(page).trim().toLowerCase();
            return target === urlValue || cleanText.includes(target) || target.includes(cleanText);
        });
    });

    if (statusBanner) statusBanner.className = "hidden";
    toggleModal(true, true);
};

// Edit User Roles Modal Trigger Function
window.editUserRoles = async function(userId) {
    currentEditingUserId = userId;

    if (allAvailableRoles.length === 0) {
        await fetchAllRoles();
    }

    const userItem = cachedUserMatrixData.find(u => u.id === userId);
    if (!userItem) {
        console.error("User record not found in cache.");
        return;
    }

    if (editUserIdInput) editUserIdInput.value = userItem.id;
    if (editUsernameInput) editUsernameInput.value = userItem.username || '';

    if (userRolesCheckboxContainer) {
        userRolesCheckboxContainer.innerHTML = '';
        allAvailableRoles.forEach(roleItem => {
            const roleName = typeof roleItem === 'string' ? roleItem : (roleItem.name || roleItem.roleName);
            const isAssigned = (userItem.roles || []).includes(roleName);

            const label = document.createElement('label');
            label.className = 'checkbox-item-label';
            label.innerHTML = `
                <input type="checkbox" name="userRoleCheckbox" value="${roleName}" class="form-checkbox" ${isAssigned ? 'checked' : ''}>
                <div class="checkbox-text-group">
                    <span class="checkbox-main-text">${createRoleBadge(roleName)}</span>
                </div>
            `;
            userRolesCheckboxContainer.appendChild(label);
        });
    }

    if (updateUserStatusBanner) updateUserStatusBanner.className = "hidden";
    toggleUserRoleModal(true);
};

window.deleteRoleMapping = async function(roleId) {
    try {
        const countResponse = await fetch(`/api/roles/getRoleUserCount?roleId=${encodeURIComponent(roleId)}`, {
            method: 'GET',
            headers: { 'Content-Type': 'application/json' }
        });
        if (!countResponse.ok) {
            throw new Error("Failed to fetch user count.");
        }

        const userCount = await countResponse.text();

        const confirmationMessage = `Are you sure you want to delete this role? There are ${userCount} users using this role.`;
        if (!confirm(confirmationMessage)) {
            return;
        }

        const deleteResponse = await fetch(`/api/roles/deleteRole?roleId=${encodeURIComponent(roleId)}`, {
            method: 'DELETE',
            headers: { 'Content-Type': 'application/json' }
        });

        const responseMessage = await deleteResponse.text();

        if (deleteResponse.ok) {
            loadRolesPages(currentRoleMappingPage);
            loadUsersRoles(currentPage);
        } else {
            alert("Failed to delete role: " + responseMessage);
        }
    } catch (error) {
        console.error("Error during deletion process:", error);
        alert("Unable to complete the deletion process due to a network error.");
    }
};

async function loadRolesPages(page = 0) {
    try {
        const searchInput = document.getElementById('roleMappingFilter');
        const currentSearch = searchInput ? searchInput.value.trim() : '';

        const response = await fetch(`/api/roles/getRolesPagesList?keyword=${encodeURIComponent(currentSearch)}&page=${page}&size=${pageSize}`);
        if (!response.ok) throw new Error('Failed to fetch role-to-page mappings');

        const data = await response.json();
        cachedRoleMappingData = data.result || [];
        currentRoleMappingPage = data.currentPage ?? page;
        totalRoleMappingPages = data.totalPages || 1;

        const roleMappingBody = document.getElementById('roleMappingBody');
        if (roleMappingBody) {
            roleMappingBody.innerHTML = '';

            if (cachedRoleMappingData.length === 0) {
                roleMappingBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 24px;">No records found</td></tr>`;
            } else {
                cachedRoleMappingData.forEach(item => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${createRoleBadge(item.roleName)}</td>
                        <td style="font-weight: 600;">${item.roleDesc || 'No description provided'}</td>
                        <td>${renderTagsList(item.pagesCanAccess)}</td>
                        <td style="text-align: right;">
                            <button class="table-action-edit" onclick="editRoleMapping(${item.id})">Edit</button>
                            <button class="table-action-delete" onclick="deleteRoleMapping(${item.id})">Remove</button>
                        </td>
                    `;
                    roleMappingBody.appendChild(tr);
                });
            }
        }

        updatePaginationUI(currentRoleMappingPage, totalRoleMappingPages, 'roleMappingPageInfoDisplay', 'roleMappingPrevBtn', 'roleMappingNextBtn', loadRolesPages);
    } catch (err) {
        console.error("Error loading role-to-page mappings:", err);
    }
}

async function loadUsersRoles(page = 0) {
    try {
        const searchInput = document.getElementById('usernameSearchInput');
        if (searchInput) currentSearch = searchInput.value.trim();

        const response = await fetch(`/api/roles/getUsersRolesList?keyword=${encodeURIComponent(currentSearch || '')}&page=${page}&size=${pageSize}`);
        if (!response.ok) throw new Error('Failed to fetch role assignments');

        const data = await response.json();
        const matrixData = data.result || [];
        cachedUserMatrixData = matrixData; // Cache matrix data for edit reference
        currentPage = data.currentPage ?? page;
        totalPages = data.totalPages || 1;

        if (roleMatrixBody) {
            roleMatrixBody.innerHTML = '';

            if (matrixData.length === 0) {
                roleMatrixBody.innerHTML = `<tr><td colspan="4" style="text-align: center; color: var(--text-muted); padding: 24px;">No records found</td></tr>`;
            } else {
                matrixData.forEach(user => {
                    const roleBadges = (user.roles || []).map(role => createRoleBadge(role, 'margin-right: 4px;')).join('');
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td style="font-weight: 500; color: var(--text-muted);">${user.id}</td>
                        <td style="font-weight: 600;">${user.username}</td>
                        <td>${roleBadges || '<span style="color: var(--text-muted); font-size: 12px;">No roles assigned</span>'}</td>
                        <td style="text-align: right;">
                            <button class="table-action-edit" onclick="editUserRoles(${user.id})">Edit</button>
                        </td>
                    `;
                    roleMatrixBody.appendChild(tr);
                });
            }
        }

        updatePaginationUI(currentPage, totalPages, 'pageInfoDisplay', 'prevPageBtn', 'nextPageBtn', loadUsersRoles);
    } catch (err) {
        console.error("Error loading role matrix:", err);
    }
}

// RUN IMMEDIATELY ON MODULE LOAD
(function init() {
    cacheDOMElements();
    initEventListeners();
    Utils.fetchAndSetUserProfile(null);
    loadRolesPages();
    loadUsersRoles();
    fetchAllRoles();
})();

window.addEventListener('dashboardEvents', (e) => {
    if (e.detail && e.detail.username) {
        Utils.fetchAndSetUserProfile(e.detail.username);
    }
});