import { useAuthStore } from '@/stores/auth';

function checkPerm(el, binding) {
  const auth = useAuthStore();
  const perms = auth.permissions;
  if (!perms || perms.length === 0) {
    el.style.display = 'none';
    return;
  }
  const required = binding.value;
  if (Array.isArray(required)) {
    if (!required.some((p) => perms.includes(p))) {
      el.style.display = 'none';
    }
  } else if (typeof required === 'string') {
    if (!perms.includes(required)) {
      el.style.display = 'none';
    }
  }
}

export default {
  install(app) {
    app.directive('permission', {
      mounted(el, binding) {
        checkPerm(el, binding);
      },
      updated(el, binding) {
        checkPerm(el, binding);
      }
    });
  }
};
