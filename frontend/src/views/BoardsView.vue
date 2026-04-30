<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { createBoard, deleteBoard, getBoard, listBoards, updateBoard } from '@/api/boards';

const loading = ref(false);
const errorMessage = ref('');
const records = ref([]);
const total = ref(0);
const page = reactive({ pageNum: 1, pageSize: 10 });
const modalMode = ref('');
const currentId = ref(null);
const form = reactive({
  brand: '',
  materialType: '',
  color: '',
  sizeType: '',
  width: '',
  length: '',
  thickness: '',
  isEnabled: 1,
  remark: ''
});

const readonly = computed(() => modalMode.value === 'detail');
const modalTitle = computed(() => {
  if (modalMode.value === 'create') return '新增板材';
  if (modalMode.value === 'edit') return '编辑板材';
  return '板材详情';
});

function resetForm(data = {}) {
  form.brand = data.brand || '';
  form.materialType = data.materialType || '';
  form.color = data.color || '';
  form.sizeType = data.sizeType || '';
  form.width = data.width || '';
  form.length = data.length || '';
  form.thickness = data.thickness || '';
  form.isEnabled = data.isEnabled ?? 1;
  form.remark = data.remark || '';
}

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  try {
    const data = await listBoards(page);
    records.value = data?.records || [];
    total.value = data?.total ?? records.value.length;
  } catch (error) {
    errorMessage.value = error.message;
  } finally {
    loading.value = false;
  }
}

function toPayload(includeStatus = false) {
  const payload = {
    brand: form.brand,
    materialType: form.materialType,
    color: form.color,
    sizeType: form.sizeType,
    width: Number(form.width),
    length: Number(form.length),
    thickness: Number(form.thickness),
    remark: form.remark
  };
  if (includeStatus) {
    payload.isEnabled = Number(form.isEnabled);
  }
  return payload;
}

function openCreate() {
  currentId.value = null;
  resetForm();
  modalMode.value = 'create';
}

async function openDetail(id) {
  const data = await getBoard(id);
  currentId.value = id;
  resetForm(data);
  modalMode.value = 'detail';
}

async function openEdit(id) {
  const data = await getBoard(id);
  currentId.value = id;
  resetForm(data);
  modalMode.value = 'edit';
}

async function submit() {
  if (modalMode.value === 'create') {
    await createBoard(toPayload(false));
  } else {
    await updateBoard(currentId.value, toPayload(true));
  }
  modalMode.value = '';
  await loadData();
}

async function remove(id) {
  if (!window.confirm('确认删除该板材？')) return;
  await deleteBoard(id);
  await loadData();
}

function nextPage() {
  if (page.pageNum * page.pageSize < total.value) {
    page.pageNum += 1;
    loadData();
  }
}

function prevPage() {
  if (page.pageNum > 1) {
    page.pageNum -= 1;
    loadData();
  }
}

onMounted(loadData);
</script>

<template>
  <div class="section-block">
    <div class="section-title">
      <div>
        <h2>板材列表</h2>
        <p>维护品牌、材质、颜色、规格和尺寸信息</p>
      </div>
      <button class="btn primary" type="button" @click="openCreate">新增板材</button>
    </div>

    <p v-if="errorMessage" class="form-error">{{ errorMessage }}</p>

    <div class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>品牌</th>
            <th>材质</th>
            <th>颜色</th>
            <th>规格</th>
            <th>尺寸</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading">
            <td colspan="8">加载中...</td>
          </tr>
          <tr v-else-if="!records.length">
            <td colspan="8">暂无板材数据</td>
          </tr>
          <tr v-for="item in records" v-else :key="item.boardId">
            <td>{{ item.boardId }}</td>
            <td>{{ item.brand }}</td>
            <td>{{ item.materialType }}</td>
            <td>{{ item.color }}</td>
            <td>{{ item.sizeType }}</td>
            <td>{{ item.length }} × {{ item.width }} × {{ item.thickness }}</td>
            <td>
              <span class="status" :class="{ off: item.isEnabled === 0 }">
                {{ item.isEnabled === 0 ? '禁用' : '启用' }}
              </span>
            </td>
            <td>
              <div class="row-actions">
                <button class="btn small ghost" type="button" @click="openDetail(item.boardId)">查看</button>
                <button class="btn small secondary" type="button" @click="openEdit(item.boardId)">编辑</button>
                <button class="btn small danger" type="button" @click="remove(item.boardId)">删除</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="pager">
      <span>共 {{ total }} 条，第 {{ page.pageNum }} 页</span>
      <button class="btn ghost" type="button" :disabled="page.pageNum <= 1" @click="prevPage">上一页</button>
      <button class="btn ghost" type="button" :disabled="page.pageNum * page.pageSize >= total" @click="nextPage">下一页</button>
    </div>
  </div>

  <div v-if="modalMode" class="modal-backdrop">
    <form class="modal large" @submit.prevent="submit">
      <div class="modal-header">
        <h3>{{ modalTitle }}</h3>
        <button class="icon-btn" type="button" @click="modalMode = ''">×</button>
      </div>
      <div class="form-grid">
        <label>
          <span>品牌</span>
          <input v-model.trim="form.brand" class="input" :readonly="readonly" required />
        </label>
        <label>
          <span>材质</span>
          <input v-model.trim="form.materialType" class="input" :readonly="readonly" required />
        </label>
        <label>
          <span>颜色</span>
          <input v-model.trim="form.color" class="input" :readonly="readonly" required />
        </label>
        <label>
          <span>规格类型</span>
          <input v-model.trim="form.sizeType" class="input" :readonly="readonly" required />
        </label>
        <label>
          <span>长度 mm</span>
          <input v-model.number="form.length" class="input" type="number" min="1" :readonly="readonly" required />
        </label>
        <label>
          <span>宽度 mm</span>
          <input v-model.number="form.width" class="input" type="number" min="1" :readonly="readonly" required />
        </label>
        <label>
          <span>厚度 mm</span>
          <input v-model.number="form.thickness" class="input" type="number" min="1" :readonly="readonly" required />
        </label>
        <label>
          <span>状态</span>
          <select v-model="form.isEnabled" class="input" :disabled="readonly || modalMode === 'create'">
            <option :value="1">启用</option>
            <option :value="0">禁用</option>
          </select>
        </label>
        <label class="wide">
          <span>备注</span>
          <textarea v-model.trim="form.remark" class="input" rows="3" :readonly="readonly"></textarea>
        </label>
      </div>
      <div class="modal-actions">
        <button class="btn ghost" type="button" @click="modalMode = ''">取消</button>
        <button v-if="!readonly" class="btn primary" type="submit">保存</button>
        <button v-else class="btn secondary" type="button" @click="modalMode = 'edit'">编辑</button>
      </div>
    </form>
  </div>
</template>
