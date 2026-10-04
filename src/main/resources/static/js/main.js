/* ============================================================
   AI Resume Screening System - Main JavaScript
   ============================================================ */

// Mobile nav toggle
document.addEventListener('DOMContentLoaded', function () {

  // Hamburger menu
  const hamburger = document.getElementById('hamburgerBtn');
  const navMenu   = document.getElementById('navMenu');
  if (hamburger && navMenu) {
    hamburger.addEventListener('click', () => navMenu.classList.toggle('open'));
  }

  // Auto-dismiss alerts after 5 seconds
  document.querySelectorAll('.alert[data-auto-dismiss]').forEach(alert => {
    setTimeout(() => {
      alert.style.opacity = '0';
      alert.style.transition = 'opacity .4s';
      setTimeout(() => alert.remove(), 400);
    }, 5000);
  });

  // File upload drag-and-drop + preview
  const uploadZone = document.getElementById('uploadZone');
  const fileInput  = document.getElementById('resumeFile');
  const fileInfo   = document.getElementById('fileInfo');
  const fileName   = document.getElementById('fileName');

  if (uploadZone && fileInput) {
    uploadZone.addEventListener('click', () => fileInput.click());

    uploadZone.addEventListener('dragover', e => {
      e.preventDefault();
      uploadZone.classList.add('drag-over');
    });
    uploadZone.addEventListener('dragleave', () => uploadZone.classList.remove('drag-over'));
    uploadZone.addEventListener('drop', e => {
      e.preventDefault();
      uploadZone.classList.remove('drag-over');
      const file = e.dataTransfer.files[0];
      if (file) handleFileSelect(file);
    });

    fileInput.addEventListener('change', () => {
      if (fileInput.files[0]) handleFileSelect(fileInput.files[0]);
    });
  }

  function handleFileSelect(file) {
    const allowed = ['application/pdf',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'];
    const ext = file.name.split('.').pop().toLowerCase();
    if (!['pdf','docx'].includes(ext)) {
      showToast('Only PDF and DOCX files are accepted.', 'error');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      showToast('File exceeds 10 MB limit.', 'error');
      return;
    }
    // Transfer to the real file input
    const dt = new DataTransfer();
    dt.items.add(file);
    fileInput.files = dt.files;

    if (fileInfo && fileName) {
      fileName.textContent = file.name + ' (' + formatBytes(file.size) + ')';
      fileInfo.style.display = 'flex';
    }
    if (uploadZone) {
      uploadZone.querySelector('.upload-zone-text').textContent = '✓ File selected';
      uploadZone.style.borderColor = 'var(--success)';
      uploadZone.style.background  = 'var(--success-light)';
    }
  }

  function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / 1048576).toFixed(1) + ' MB';
  }

  // Skills multi-select (tag input)
  document.querySelectorAll('.skill-input-container').forEach(container => {
    const input    = container.querySelector('.skill-text-input');
    const tagsDiv  = container.querySelector('.skill-tags-input');
    const hiddenContainer = container.querySelector('.skill-hidden-inputs');
    const fieldName = container.dataset.fieldName || 'skills';

    if (!input || !tagsDiv) return;

    function addTag(value) {
      const v = value.trim();
      if (!v) return;
      // Avoid duplicates
      if (tagsDiv.querySelector(`[data-skill="${v.toLowerCase()}"]`)) return;

      const tag = document.createElement('span');
      tag.className = 'skill-tag';
      tag.dataset.skill = v.toLowerCase();
      tag.innerHTML = v + ' <button type="button" onclick="this.parentElement.remove(); this.parentElement.nextElementSibling && removeHiddenInput(\'' + fieldName + '\', \'' + v + '\')">×</button>';
      tagsDiv.appendChild(tag);

      if (hiddenContainer) {
        const hidden = document.createElement('input');
        hidden.type  = 'hidden';
        hidden.name  = fieldName;
        hidden.value = v;
        hiddenContainer.appendChild(hidden);
      }
      input.value = '';
    }

    input.addEventListener('keydown', e => {
      if (e.key === 'Enter' || e.key === ',') {
        e.preventDefault();
        addTag(input.value);
      }
    });

    // Datalist suggestion click
    input.addEventListener('change', () => addTag(input.value));
  });

  // Confirm delete dialogs
  document.querySelectorAll('[data-confirm]').forEach(el => {
    el.addEventListener('click', e => {
      if (!confirm(el.dataset.confirm)) e.preventDefault();
    });
  });

  // Score ring animation
  document.querySelectorAll('.score-ring[data-score]').forEach(ring => {
    const score = parseFloat(ring.dataset.score);
    const circle = ring.querySelector('.score-ring-progress');
    if (circle) {
      const radius = parseFloat(circle.getAttribute('r'));
      const circumference = 2 * Math.PI * radius;
      circle.style.strokeDasharray = circumference;
      circle.style.strokeDashoffset = circumference;

      // Determine colour
      let colour = '#dc2626';
      if (score >= 70) colour = '#16a34a';
      else if (score >= 40) colour = '#d97706';
      circle.style.stroke = colour;

      // Animate
      requestAnimationFrame(() => {
        circle.style.transition = 'stroke-dashoffset 1s ease';
        circle.style.strokeDashoffset = circumference - (score / 100) * circumference;
      });
    }
  });

  // Progress bar animation
  document.querySelectorAll('.progress-bar-fill[data-width]').forEach(bar => {
    const w = bar.dataset.width;
    requestAnimationFrame(() => {
      bar.style.width = '0%';
      setTimeout(() => { bar.style.width = w + '%'; }, 100);
    });
  });

  // Toast notifications
  window.showToast = function(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toast-container';
      container.style.cssText = 'position:fixed;bottom:1.5rem;right:1.5rem;z-index:9999;display:flex;flex-direction:column;gap:.5rem;';
      document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    const colours = { success:'#16a34a', error:'#dc2626', info:'#0891b2', warning:'#d97706' };
    toast.style.cssText = `background:#fff;border-left:4px solid ${colours[type]||colours.info};
      box-shadow:0 4px 12px rgba(0,0,0,.15);border-radius:6px;padding:.875rem 1rem;
      min-width:280px;font-size:.875rem;color:#374151;animation:slideIn .25s ease;`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => { toast.style.opacity='0'; toast.style.transition='opacity .3s'; setTimeout(()=>toast.remove(),300); }, 3500);
  };

  window.removeHiddenInput = function(fieldName, value) {
    document.querySelectorAll(`input[type=hidden][name="${fieldName}"][value="${value}"]`)
            .forEach(el => el.remove());
  };
});
