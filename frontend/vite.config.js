import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const backendUrl = env.VITE_BACKEND_URL || 'http://localhost:8080';

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': '/src'
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: backendUrl,
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, '')
        }
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) return undefined;
            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return 'vendor-element-plus';
            }
            if (id.includes('vue-echarts')) {
              return 'vendor-vue-echarts';
            }
            if (id.includes('zrender')) {
              return 'vendor-zrender';
            }
            if (id.includes('echarts')) {
              return 'vendor-echarts';
            }
            if (id.includes('vue') || id.includes('pinia')) {
              return 'vendor-vue';
            }
            return 'vendor';
          }
        }
      }
    }
  };
});
