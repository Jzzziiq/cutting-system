<script setup>
import { nextTick, reactive, ref, watch } from 'vue';
import { solveAlgorithm } from '@/api/algorithm';

const loading = ref(false);
const errorMessage = ref('');
const canvasRef = ref(null);
const activeIndex = ref(0);
const solutions = ref([]);
const form = reactive({
  L: 100,
  W: 50,
  gapDistance: 0,
  rotateEnable: false,
  squareList: [
    { id: 'item1', l: 20, w: 15 },
    { id: 'item2', l: 30, w: 20 }
  ]
});

function addSquare() {
  form.squareList.push({ id: `item${form.squareList.length + 1}`, l: 10, w: 10 });
}

function removeSquare(index) {
  if (form.squareList.length > 1) {
    form.squareList.splice(index, 1);
  }
}

function buildPayload() {
  return {
    L: Number(form.L),
    W: Number(form.W),
    rotateEnable: Boolean(form.rotateEnable),
    gapDistance: Number(form.gapDistance || 0),
    squareList: form.squareList.map((item, index) => ({
      id: item.id || `item${index + 1}`,
      l: Number(item.l),
      w: Number(item.w)
    }))
  };
}

async function submit() {
  errorMessage.value = '';
  loading.value = true;
  try {
    const payload = buildPayload();
    if (payload.L <= 0 || payload.W <= 0 || payload.gapDistance < 0) {
      throw new Error('容器尺寸必须大于 0，间隙不能小于 0');
    }
    if (payload.squareList.some((item) => item.l <= 0 || item.w <= 0)) {
      throw new Error('所有矩形的长宽都必须大于 0');
    }
    solutions.value = await solveAlgorithm(payload);
    activeIndex.value = 0;
    await nextTick();
    drawSolution();
  } catch (error) {
    solutions.value = [];
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
}

function drawSolution() {
  const canvas = canvasRef.value;
  const solution = solutions.value[activeIndex.value];
  if (!canvas || !solution) return;

  const ctx = canvas.getContext('2d');
  const width = canvas.width;
  const height = canvas.height;
  const padding = 24;
  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = '#f8fafc';
  ctx.fillRect(0, 0, width, height);

  const containerLength = solution.containerLength || form.L;
  const containerWidth = solution.containerWidth || form.W;
  const scale = Math.min((width - padding * 2) / containerLength, (height - padding * 2) / containerWidth);
  const originX = (width - containerLength * scale) / 2;
  const originY = (height - containerWidth * scale) / 2;

  ctx.strokeStyle = '#1f2937';
  ctx.lineWidth = 2;
  ctx.strokeRect(originX, originY, containerLength * scale, containerWidth * scale);

  const colors = ['#2563eb', '#0f766e', '#c2410c', '#7c3aed', '#b91c1c', '#4d7c0f'];
  (solution.placeSquareList || []).forEach((item, index) => {
    const x = originX + Number(item.x || 0) * scale;
    const y = originY + Number(item.y || 0) * scale;
    const l = Number(item.l || 0) * scale;
    const w = Number(item.w || 0) * scale;
    ctx.fillStyle = colors[index % colors.length];
    ctx.globalAlpha = 0.82;
    ctx.fillRect(x, y, l, w);
    ctx.globalAlpha = 1;
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 1;
    ctx.strokeRect(x, y, l, w);
    ctx.fillStyle = '#ffffff';
    ctx.font = '12px sans-serif';
    ctx.fillText(item.id || String(index + 1), x + 6, y + 16);
  });
}

watch(activeIndex, () => nextTick(drawSolution));
</script>

<template>
  <div class="algorithm-grid">
    <form class="section-block" @submit.prevent="submit">
      <div class="section-title">
        <div>
          <h2>排样参数</h2>
          <p>输入板材容器尺寸和待切割矩形信息</p>
        </div>
        <button class="btn primary" type="submit" :disabled="loading">
          {{ loading ? '计算中...' : '开始计算' }}
        </button>
      </div>

      <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

      <div class="form-grid">
        <label>
          <span>容器长度</span>
          <input v-model.number="form.L" class="input" type="number" min="1" required />
        </label>
        <label>
          <span>容器宽度</span>
          <input v-model.number="form.W" class="input" type="number" min="1" required />
        </label>
        <label>
          <span>间隙距离</span>
          <input v-model.number="form.gapDistance" class="input" type="number" min="0" />
        </label>
        <label>
          <span>允许旋转</span>
          <select v-model="form.rotateEnable" class="input">
            <option :value="false">否</option>
            <option :value="true">是</option>
          </select>
        </label>
      </div>

      <div class="square-list">
        <div class="subsection-title">
          <h3>矩形列表</h3>
          <button class="btn small secondary" type="button" @click="addSquare">添加矩形</button>
        </div>
        <div v-for="(item, index) in form.squareList" :key="index" class="square-row">
          <input v-model.trim="item.id" class="input" placeholder="编号" />
          <input v-model.number="item.l" class="input" type="number" min="1" placeholder="长度" required />
          <input v-model.number="item.w" class="input" type="number" min="1" placeholder="宽度" required />
          <button class="btn small ghost" type="button" @click="removeSquare(index)">删除</button>
        </div>
      </div>
    </form>

    <section class="section-block">
      <div class="section-title">
        <div>
          <h2>结果可视化</h2>
          <p>展示每块矩形在板材中的放置位置</p>
        </div>
      </div>

      <div v-if="solutions.length" class="tabs">
        <button
          v-for="(solution, index) in solutions"
          :key="index"
          class="tab"
          :class="{ active: activeIndex === index }"
          type="button"
          @click="activeIndex = index"
        >
          方案 {{ index + 1 }} · {{ ((solution.rate || 0) * 100).toFixed(1) }}%
        </button>
      </div>

      <canvas ref="canvasRef" class="layout-canvas" width="720" height="420"></canvas>

      <div v-if="!solutions.length" class="empty-state">提交参数后显示排样结果</div>
    </section>
  </div>

  <div v-if="solutions.length" class="section-block">
    <div class="section-title">
      <h2>放置明细</h2>
    </div>
    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>编号</th>
            <th>X</th>
            <th>Y</th>
            <th>长度</th>
            <th>宽度</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(item, index) in solutions[activeIndex]?.placeSquareList || []" :key="index">
            <td>{{ item.id || index + 1 }}</td>
            <td>{{ item.x }}</td>
            <td>{{ item.y }}</td>
            <td>{{ item.l }}</td>
            <td>{{ item.w }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
