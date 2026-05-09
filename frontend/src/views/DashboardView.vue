<script setup>
import { onMounted, ref, watch } from 'vue';
import { getSummary, getOrderTrend, getOrderStatusDist, getUtilizationTrend } from '@/api/dashboard';
import VChart from 'vue-echarts';
import { use } from 'echarts/core';
import { CanvasRenderer } from 'echarts/renderers';
import { LineChart, PieChart, BarChart } from 'echarts/charts';
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components';

use([CanvasRenderer, LineChart, PieChart, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent]);

const loading = ref(true);
const summary = ref({});
const trendDays = ref(7);
const orderTrend = ref({ labels: [], data: [] });
const orderStatusDist = ref({ labels: [], data: [] });
const utilizationTrend = ref({ labels: [], data: [] });

async function loadData() {
  loading.value = true;
  try {
    const [s, ot, osd] = await Promise.all([
      getSummary(),
      getOrderTrend(trendDays.value),
      getOrderStatusDist()
    ]);
    summary.value = s;
    orderTrend.value = ot;
    orderStatusDist.value = osd;
  } finally {
    loading.value = false;
  }
}

async function loadUtilization() {
  try {
    utilizationTrend.value = await getUtilizationTrend(trendDays.value);
  } catch { /* ignore */ }
}

function fmtRate(v) { return v != null ? (Number(v) * 100).toFixed(1) + '%' : '-'; }

const orderTrendOption = ref({});
const orderStatusOption = ref({});
const utilizationOption = ref({});

watch([orderTrend, orderStatusDist, utilizationTrend], () => {
  orderTrendOption.value = {
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 16, top: 12, bottom: 24 },
    xAxis: { type: 'category', data: orderTrend.value.labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
    series: [{ data: orderTrend.value.data, type: 'line', smooth: true, areaStyle: { opacity: 0.15 }, itemStyle: { color: '#3b82f6' } }]
  };
  orderStatusOption.value = {
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', right: 8, top: 8, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['40%', '72%'], center: ['38%', '52%'], avoidLabelOverlap: false,
      label: { show: false }, emphasis: { label: { show: true, fontSize: 13 } },
      data: orderStatusDist.value.labels.map((name, i) => ({ name, value: orderStatusDist.value.data[i] }))
    }]
  };
  utilizationOption.value = {
    tooltip: { trigger: 'axis', valueFormatter: v => (v * 100).toFixed(1) + '%' },
    grid: { left: 46, right: 16, top: 12, bottom: 24 },
    xAxis: { type: 'category', data: utilizationTrend.value.labels, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, formatter: v => (v * 100).toFixed(0) + '%' } },
    series: [{ data: utilizationTrend.value.data, type: 'bar', barWidth: 16, itemStyle: { color: '#10b981', borderRadius: [4,4,0,0] } }]
  };
}, { deep: true });

async function switchTrend(days) {
  trendDays.value = days;
  const [ot, ut] = await Promise.all([getOrderTrend(days), getUtilizationTrend(days)]);
  orderTrend.value = ot;
  utilizationTrend.value = ut;
}

onMounted(async () => {
  await loadData();
  await loadUtilization();
});
</script>

<template>
  <div>
    <!-- metric cards -->
    <div class="dashboard-grid">
      <article class="metric-card">
        <span>订单总数</span>
        <strong>{{ loading ? '...' : summary.orderTotal }}</strong>
      </article>
      <article class="metric-card">
        <span>活跃订单</span>
        <strong>{{ loading ? '...' : summary.activeOrders }}</strong>
      </article>
      <article class="metric-card">
        <span>今日新增</span>
        <strong>{{ loading ? '...' : summary.orderToday }}</strong>
      </article>
      <article class="metric-card">
        <span>本月新增</span>
        <strong>{{ loading ? '...' : summary.orderThisMonth }}</strong>
      </article>
      <article class="metric-card">
        <span>待处理任务</span>
        <strong>{{ loading ? '...' : summary.taskPending }}</strong>
      </article>
      <article class="metric-card">
        <span>生产中任务</span>
        <strong>{{ loading ? '...' : summary.taskInProgress }}</strong>
      </article>
      <article class="metric-card">
        <span>本周完成</span>
        <strong>{{ loading ? '...' : summary.taskCompletedThisWeek }}</strong>
      </article>
      <article class="metric-card">
        <span>客户总数</span>
        <strong>{{ loading ? '...' : summary.customerCount }}</strong>
      </article>
      <article class="metric-card">
        <span>平均利用率</span>
        <strong>{{ loading ? '...' : fmtRate(summary.avgUtilization) }}</strong>
      </article>
    </div>

    <!-- charts row -->
    <div class="charts-grid">
      <section class="chart-block">
        <div class="chart-header">
          <h3>订单趋势</h3>
          <div class="tabs">
            <button :class="['btn', 'small', trendDays === 7 ? 'primary' : 'ghost']" @click="switchTrend(7)">近7天</button>
            <button :class="['btn', 'small', trendDays === 30 ? 'primary' : 'ghost']" @click="switchTrend(30)">近30天</button>
          </div>
        </div>
        <v-chart :option="orderTrendOption" style="height:220px" autoresize />
      </section>
      <section class="chart-block">
        <h3>订单状态分布</h3>
        <v-chart :option="orderStatusOption" style="height:260px" autoresize />
      </section>
    </div>

    <div class="charts-grid" style="margin-top:16px">
      <section class="chart-block">
        <h3>利用率趋势</h3>
        <v-chart :option="utilizationOption" style="height:220px" autoresize />
      </section>
      <section class="section-block">
        <div class="section-title"><h2>常用入口</h2></div>
        <div class="quick-actions">
          <router-link class="btn secondary" to="/customers">客户管理</router-link>
          <router-link class="btn secondary" to="/boards">板材管理</router-link>
          <router-link class="btn secondary" to="/production-board">生产看板</router-link>
          <router-link class="btn primary" to="/algorithm">算法排样</router-link>
        </div>
      </section>
    </div>
  </div>
</template>
