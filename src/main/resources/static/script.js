document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('shorten-form');
    const originalUrlInput = document.getElementById('original-url');
    const customAliasInput = document.getElementById('custom-alias');
    const expirationSelect = document.getElementById('expiration');
    const submitBtn = document.getElementById('submit-btn');
    const spinner = submitBtn.querySelector('.spinner');
    const btnText = submitBtn.querySelector('.btn-text');

    const resultBox = document.getElementById('result-box');
    const shortUrlOutput = document.getElementById('short-url-output');
    const copyBtn = document.getElementById('copy-btn');
    const viewStatsBtn = document.getElementById('view-stats-btn');
    const deleteUrlBtn = document.getElementById('delete-url-btn');

    const statsSection = document.getElementById('stats-section');
    const statsShortCode = document.getElementById('stats-short-code');
    const statsClicks = document.getElementById('stats-clicks');
    const statsCreated = document.getElementById('stats-created');
    const statsExpires = document.getElementById('stats-expires');
    const statsOriginalLink = document.getElementById('stats-original-link');

    const healthDot = document.getElementById('health-dot');
    const healthText = document.getElementById('health-text');
    const toast = document.getElementById('toast');

    let currentShortCode = '';

    // Check system health on load
    checkHealth();
    setInterval(checkHealth, 30000);

    // Form submission
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const originalUrl = originalUrlInput.value.trim();
        const customAlias = customAliasInput.value.trim();
        const expiresInMinutes = expirationSelect.value ? parseInt(expirationSelect.value) : null;

        if (!originalUrl) {
            showToast('Please enter a valid URL', 'error');
            return;
        }

        setLoading(true);
        resultBox.classList.add('hidden');
        statsSection.classList.add('hidden');

        try {
            const payload = { originalUrl };
            if (customAlias) payload.customAlias = customAlias;
            if (expiresInMinutes) payload.expiresInMinutes = expiresInMinutes;

            const response = await fetch('/api/urls', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Failed to shorten URL');
            }

            currentShortCode = data.shortCode;
            shortUrlOutput.value = data.shortUrl;
            resultBox.classList.remove('hidden');
            showToast('Short URL generated successfully!', 'success');
        } catch (err) {
            showToast(err.message, 'error');
        } finally {
            setLoading(false);
        }
    });

    // Copy URL to Clipboard
    copyBtn.addEventListener('click', () => {
        if (!shortUrlOutput.value) return;
        navigator.clipboard.writeText(shortUrlOutput.value)
            .then(() => {
                showToast('Copied to clipboard!', 'success');
            })
            .catch(() => {
                showToast('Failed to copy', 'error');
            });
    });

    // View Analytics
    viewStatsBtn.addEventListener('click', async () => {
        if (!currentShortCode) return;

        try {
            const response = await fetch(`/api/urls/${currentShortCode}/stats`);
            const data = await response.json();

            if (!response.ok) {
                throw new Error(data.message || 'Failed to fetch analytics');
            }

            statsShortCode.textContent = data.shortCode;
            statsClicks.textContent = data.clickCount;
            statsCreated.textContent = formatDate(data.createdAt);
            statsExpires.textContent = data.expiresAt ? formatDate(data.expiresAt) : 'Never';
            statsOriginalLink.textContent = data.originalUrl;
            statsOriginalLink.href = data.originalUrl;

            statsSection.classList.remove('hidden');
            statsSection.scrollIntoView({ behavior: 'smooth' });
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Delete Short URL
    deleteUrlBtn.addEventListener('click', async () => {
        if (!currentShortCode) return;
        if (!confirm(`Are you sure you want to delete short URL '${currentShortCode}'?`)) return;

        try {
            const response = await fetch(`/api/urls/${currentShortCode}`, {
                method: 'DELETE'
            });

            if (!response.ok && response.status !== 240) {
                const data = await response.json().catch(() => ({}));
                throw new Error(data.message || 'Failed to delete URL');
            }

            resultBox.classList.add('hidden');
            statsSection.classList.add('hidden');
            originalUrlInput.value = '';
            customAliasInput.value = '';
            expirationSelect.value = '';
            currentShortCode = '';

            showToast('Short URL deleted successfully!', 'success');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Check System Health
    async function checkHealth() {
        try {
            const response = await fetch('/health');
            const data = await response.json();

            healthDot.className = 'status-dot';
            if (data.status === 'UP') {
                healthDot.classList.add('up');
                healthText.textContent = 'System: Fully Operational (DB & Redis UP)';
            } else if (data.status === 'DEGRADED') {
                healthDot.classList.add('degraded');
                healthText.textContent = 'System: Degraded (Redis DOWN, DB UP)';
            } else {
                healthDot.classList.add('down');
                healthText.textContent = 'System: Unavailable';
            }
        } catch (err) {
            healthDot.className = 'status-dot down';
            healthText.textContent = 'System: Offline';
        }
    }

    // Helper functions
    function setLoading(isLoading) {
        if (isLoading) {
            submitBtn.disabled = true;
            spinner.classList.remove('hidden');
            btnText.textContent = 'Shortening...';
        } else {
            submitBtn.disabled = false;
            spinner.classList.add('hidden');
            btnText.textContent = 'Shorten URL';
        }
    }

    function showToast(message, type = 'success') {
        toast.textContent = message;
        toast.className = `toast ${type}`;
        setTimeout(() => {
            toast.className = 'toast hidden';
        }, 4000);
    }

    function formatDate(dateString) {
        if (!dateString) return 'N/A';
        const d = new Date(dateString);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }
});
