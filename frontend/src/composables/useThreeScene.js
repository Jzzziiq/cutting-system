import { ref, shallowRef, onBeforeUnmount } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

export function useThreeScene() {
  const canvasRef = ref(null);
  let renderer = null;
  let scene = null;
  let camera = null;
  let controls = null;
  let animationId = null;
  const boardMeshes = shallowRef(new Map());
  const edgeLines = shallowRef(new Map());
  let highlightLine = null;

  function init(canvas) {
    const width = canvas.clientWidth || 800;
    const height = canvas.clientHeight || 600;

    renderer = new THREE.WebGLRenderer({ canvas, antialias: true });
    renderer.setSize(width, height, false);
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.shadowMap.enabled = true;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;

    scene = new THREE.Scene();
    scene.background = new THREE.Color(0xf1f5f9);
    scene.fog = new THREE.Fog(0xf1f5f9, 2000, 6000);

    camera = new THREE.PerspectiveCamera(45, width / height, 10, 10000);
    camera.position.set(1200, 800, 1400);

    controls = new OrbitControls(camera, renderer.domElement);
    controls.target.set(0, 1000, 0);
    controls.update();
    controls.enableDamping = true;
    controls.dampingFactor = 0.1;

    // Lights
    scene.add(new THREE.AmbientLight(0xffffff, 0.6));
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
    dirLight.position.set(2000, 4000, 3000);
    dirLight.castShadow = true;
    dirLight.shadow.mapSize.set(1024, 1024);
    scene.add(dirLight);

    const fillLight = new THREE.DirectionalLight(0xffffff, 0.2);
    fillLight.position.set(-1000, 500, -500);
    scene.add(fillLight);

    // Grid
    const grid = new THREE.GridHelper(3000, 20, 0x94a3b8, 0xe2e8f0);
    scene.add(grid);

    animate();
  }

  function animate() {
    animationId = requestAnimationFrame(animate);
    controls.update();
    if (renderer && scene && camera) {
      renderer.render(scene, camera);
    }
  }

  const typeColors = {
    side: 0x3b82f6, layer: 0xf59e0b, door: 0x10b981,
    back: 0x94a3b8, top: 0x8b5cf6, bottom: 0x8b5cf6
  };

  function buildCabinet(boards) {
    clearScene();

    boards.forEach(board => {
      const t = board.thickness || 18;
      const w = board.designWidth || 600;
      const h = board.designLength || 2200;
      const pos = board.position || { x: 0, y: 0, z: 0 };
      const rot = board.rotation || { x: 0, y: 0, z: 0 };

      const geo = new THREE.BoxGeometry(w, t, h);
      const mat = new THREE.MeshStandardMaterial({
        color: typeColors[board.type] || 0x94a3b8,
        roughness: 0.6, metalness: 0.1
      });
      const mesh = new THREE.Mesh(geo, mat);
      mesh.position.set(pos.x, pos.y, pos.z);
      mesh.rotation.set(rot.x, rot.y, rot.z);
      mesh.castShadow = true;
      mesh.receiveShadow = true;
      mesh.userData = { boardId: board.id, boardData: board };
      scene.add(mesh);
      boardMeshes.value.set(board.id, mesh);
    });

    // Fit camera
    const box = new THREE.Box3().setFromObjects(scene.children.filter(c => c.isMesh));
    const center = new THREE.Vector3();
    box.getCenter(center);
    controls.target.copy(center);
    const size = new THREE.Vector3();
    box.getSize(size);
    const maxDim = Math.max(size.x, size.y, size.z);
    const dist = maxDim * 2.5;
    camera.position.set(center.x + dist * 0.6, center.y + dist * 0.4, center.z + dist * 0.6);
    controls.update();
  }

  function clearScene() {
    boardMeshes.value.forEach(m => scene.remove(m));
    boardMeshes.value.clear();
    edgeLines.value.forEach(l => scene.remove(l));
    edgeLines.value.clear();
    if (highlightLine) { scene.remove(highlightLine); highlightLine = null; }
  }

  function highlight(boardId) {
    const mesh = boardMeshes.value.get(boardId);
    if (!mesh) return;
    removeHighlight();
    const edgeGeo = new THREE.EdgesGeometry(mesh.geometry);
    const lineMat = new THREE.LineBasicMaterial({ color: 0xef4444, linewidth: 2 });
    highlightLine = new THREE.LineSegments(edgeGeo, lineMat);
    highlightLine.position.copy(mesh.position);
    highlightLine.rotation.copy(mesh.rotation);
    scene.add(highlightLine);
  }

  function removeHighlight() {
    if (highlightLine) { scene.remove(highlightLine); highlightLine = null; }
  }

  const raycaster = new THREE.Raycaster();

  function onClick(event, callback) {
    if (!canvasRef.value || !camera) return;
    const rect = canvasRef.value.getBoundingClientRect();
    const mouse = new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1
    );
    raycaster.setFromCamera(mouse, camera);
    const meshes = Array.from(boardMeshes.value.values());
    const intersects = raycaster.intersectObjects(meshes);
    if (intersects.length > 0) {
      const obj = intersects[0].object;
      if (obj.userData.boardData) {
        callback(obj.userData.boardData);
      }
    }
  }

  function resize() {
    if (!canvasRef.value || !renderer || !camera) return;
    const w = canvasRef.value.clientWidth;
    const h = canvasRef.value.clientHeight;
    if (w === 0 || h === 0) return;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  }

  function dispose() {
    if (animationId) cancelAnimationFrame(animationId);
    clearScene();
    if (controls) controls.dispose();
    if (renderer) renderer.dispose();
  }

  return { canvasRef, init, buildCabinet, clearScene, highlight, removeHighlight, onClick, resize, dispose };
}
