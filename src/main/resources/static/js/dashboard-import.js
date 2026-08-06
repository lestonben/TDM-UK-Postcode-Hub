import * as Utils from '/js/utils.js';

let uploadForm, fileInput, dropzoneArea, uploadText, startUploadBtn, jobTableBody, emptyJobRow;

function cacheDOMElements() {
    uploadForm = document.getElementById('uploadForm');
    fileInput = document.getElementById('fileInput');
    dropzoneArea = document.getElementById('dropzoneArea');
    uploadText = document.getElementById('uploadText');
    startUploadBtn = document.getElementById('startUploadBtn');
    jobTableBody = document.getElementById('jobTableBody');
    emptyJobRow = document.getElementById('emptyJobRow');
}

function validateUploadFormInputs() {
    if (startUploadBtn && fileInput) {
        const hasFile = fileInput.files && fileInput.files.length > 0;
        startUploadBtn.disabled = !hasFile;
    }
}

function handleFileSelection(file) {
    if (!file) {
        uploadText.textContent = "Click to browse or drag and drop your file here";
        uploadText.style.color = "var(--text-main)";
        fileInput.value = "";
    } else if (!file.name.endsWith('.xlsx')) {
        uploadText.textContent = "❌ Invalid file type. Please upload a .xlsx file.";
        uploadText.style.color = "#ef4444";
        fileInput.value = "";
    } else {
        uploadText.textContent = "Selected: " + file.name;
        uploadText.style.color = "var(--primary-blue)";
    }
    validateUploadFormInputs();
}

// --- NEW: Fetch existing jobs on page load ---
async function fetchAllJobs() {
    try {
        const response = await fetch('/api/postcodes/jobs');

        if (!response.ok)
            throw new Error('Failed to fetch jobs');
        
        const data = await response.json();
        const jobs = data.result;
        
        if (jobs.length > 0) {
            jobTableBody.innerHTML = ''; // Clear empty state
            jobs.forEach(job => {
                jobTableBody.appendChild(createJobRow(job));
            });

            // Target ONLY the top-most job (the first item in the array) for polling
            const topJob = jobs[0];
            if (topJob && topJob.status === 'PROCESSING') {
                pollJobStatus(topJob.jobId);
            }
        }
    } catch (error) {
        console.error('Error loading jobs:', error);
    }
}

// --- NEW: HTML Row Generator ---
function createJobRow(job) {
    const tr = document.createElement('tr');
    tr.id = `job-${job.jobId}`;
    
    let badgeBg = '#e2e8f0';
    let badgeText = '#475569';
    let barColor = '#3b82f6';

    if (job.status === 'COMPLETED') {
        badgeBg = '#dcfce7'; badgeText = '#166534'; barColor = '#22c55e';
    } else if (job.status.includes('FAILED')) {
        badgeBg = '#fee2e2'; badgeText = '#991b1b'; barColor = '#ef4444';
    }

    let percentage = 0;
    if (job.totalRows && job.totalRows > 0) {
        percentage = Math.round((job.processedRows / job.totalRows) * 100);
    }

    tr.innerHTML = `
        <td style="font-family: monospace; font-size: 0.85rem; color: #64748b;">
            ${job.jobId.substring(0, 8)}...
        </td>
        <td style="font-weight: 500;">${job.fileName}</td>
        <td>
            <div style="display: flex; flex-direction: column; gap: 4px; width: 100%; max-width: 200px;">
                <span class="progress-text" style="font-size: 0.8rem; color: #475569;">
                    ${(job.processedRows || 0).toLocaleString()} / ${(job.totalRows || 0).toLocaleString()} (${percentage}%)
                </span>
                <div style="width: 100%; height: 6px; background: #e2e8f0; border-radius: 4px; overflow: hidden;">
                    <div class="progress-fill" style="width: ${percentage}%; height: 100%; background: ${barColor}; transition: width 0.3s ease;"></div>
                </div>
            </div>
        </td>
        <td>
            <span class="status-badge" style="padding: 4px 8px; border-radius: 999px; font-size: 0.75rem; font-weight: 600; background: ${badgeBg}; color: ${badgeText};">
                ${job.status}
            </span>
        </td>
        <td class="submitted-by-cell" style="font-weight: 500;">${job.submittedBy}</td>
        <td>
            <button onclick="alert('View details for ${job.jobId}')" style="background: none; border: none; color: #3b82f6; cursor: pointer; font-size: 0.85rem; font-weight: 500;">
                Details
            </button>
        </td>
    `;
    return tr;
}

// --- NEW: Polling Mechanism ---
async function pollJobStatus(jobId) {
    const row = document.getElementById(`job-${jobId}`);
    if (!row) return;

    // Poll every 2.5 seconds
    const interval = setInterval(async () => {
        try {
            const response = await fetch(`/api/postcodes/jobs/status/${jobId}`);
            if (!response.ok) throw new Error('Failed to fetch status');

            const data = await response.json();
            // Handle wrapper response structure if your backend returns { result: job } or just the job object directly
            const job = data.result ? data.result : data;

            let percentage = 0;
            if (job.totalRows && job.totalRows > 0) {
                percentage = Math.round((job.processedRows / job.totalRows) * 100);
            }

            // Update DOM Elements safely
            row.querySelector('.submitted-by-cell').textContent = `${job.submittedBy}`;
            row.querySelector('.progress-text').textContent = `${(job.processedRows || 0).toLocaleString()} / ${(job.totalRows || 0).toLocaleString()} (${percentage}%)`;
            row.querySelector('.progress-fill').style.width = `${percentage}%`;

            const statusBadge = row.querySelector('.status-badge');
            statusBadge.textContent = job.status;

            // Stop polling once the job is finished or failed
            if (job.status.includes('COMPLETED') || job.status.includes('FAILED')) {
                clearInterval(interval);

                if (job.status === 'COMPLETED') {
                    statusBadge.style.backgroundColor = '#dcfce7';
                    statusBadge.style.color = '#166534';
                    row.querySelector('.progress-fill').style.backgroundColor = '#22c55e';
                } else {
                    statusBadge.style.backgroundColor = '#fee2e2';
                    statusBadge.style.color = '#991b1b';
                    row.querySelector('.progress-fill').style.backgroundColor = '#ef4444';
                }
            }

        } catch (error) {
            console.error(`Error polling job ${jobId}:`, error);
            clearInterval(interval); // Stop interval on network failure to avoid spamming errors
        }
    }, 2500);
}

function initEventListeners() {
    Utils.initSidebarToggle();
    Utils.initLogOutFunction();

    // Click dropzone triggers file input browser
    if (dropzoneArea && fileInput) {
        dropzoneArea.addEventListener('click', () => {
            fileInput.click();
        });
    }

    // File input change tracker
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            handleFileSelection(this.files[0]);
        });
    }

    // Drag and drop event handlers
    if (dropzoneArea) {
        ['dragenter', 'dragover'].forEach(eventName => {
            dropzoneArea.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropzoneArea.classList.add('drag-active');
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            dropzoneArea.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropzoneArea.classList.remove('drag-active');
            }, false);
        });

        dropzoneArea.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            const files = dt.files;
            if (files.length > 0) {
                fileInput.files = files;
                handleFileSelection(files[0]);
            }
        });
    }

    // --- Batch Upload Form Handler (UPDATED) ---
    if (uploadForm) {
        uploadForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const file = fileInput.files[0];
            if (!file) return;

            const formData = new FormData();
            formData.append('file', file);

            startUploadBtn.disabled = true;
            uploadText.textContent = "Uploading file to server...";
            uploadText.style.color = "var(--text-main)";

            try {
                // Ensure this matches your Controller's exact endpoint mapping
                const response = await fetch('/api/postcodes/jobs/import', {
                    method: 'POST',
                    headers: {
                        'X-XSRF-TOKEN': Utils.getCsrfToken()
                    },
                    body: formData
                });

                const jobId = await response.text();

                if (response.ok) {
                    // Reset Upload UI on successful handoff
                    uploadText.textContent = "✅ Upload complete. Processing in background.";
                    uploadText.style.color = "#16a34a";
                    uploadForm.reset();
                    validateUploadFormInputs();
                    
                    // Remove the empty table placeholder if it exists
                    if (emptyJobRow) {
                        emptyJobRow.remove();
                        emptyJobRow = null; 
                    }
                    
                    // Generate initial row data and inject it at the top of the table
                    const initialJobData = {
                        jobId: jobId,
                        fileName: file.name,
                        status: 'PROCESSING',
                        processedRows: 0,
                        totalRows: 0,
                        submittedBy: '-'
                    };
                    
                    const newRow = createJobRow(initialJobData);
                    jobTableBody.prepend(newRow);
                    
                    // Start polling the backend tracker
                    pollJobStatus(jobId);
                } else {
                    uploadText.textContent = "❌ Upload failed: " + jobId; // jobId acts as error message here
                    uploadText.style.color = "#ef4444";
                }
            } catch (err) {
                uploadText.textContent = "❌ Unable to process batch import API. Network error.";
                uploadText.style.color = "#ef4444";
            } finally {
                // Keep button disabled until a new file is chosen
                if (!fileInput.files || fileInput.files.length === 0) {
                    startUploadBtn.disabled = true;
                }
            }
        });
    }
}

// RUN IMMEDIATELY ON MODULE LOAD
(function init() {
    cacheDOMElements();
    initEventListeners();
    validateUploadFormInputs();
    Utils.fetchAndSetUserProfile(null);
    fetchAllJobs(); // Load existing table data immediately
})();

window.addEventListener('dashboardEvents', (e) => {
    if (e.detail && e.detail.username) {
        currentUsername = e.detail.username;
        Utils.fetchAndSetUserProfile(e.detail.username);
    }
});