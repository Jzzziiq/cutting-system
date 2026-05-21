import { ref, shallowRef } from 'vue';
import * as THREE from 'three';
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js';

export function useThreeScene() {
  const canvasRef = ref(null);
  let renderer = null;
  let scene = null;
  let camera = null;
  let controls = null;
  let animationId = null;
  let highlightLine = null;
  const boardMeshes = shallowRef(new Map());
  const edgeLines = shallowRef(new Map());

  function init(canvas) {
    dispose();
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

    scene.add(new THREE.AmbientLight(0xffffff, 0.6));
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.8);
    dirLight.position.set(2000, 4000, 3000);
    dirLight.castShadow = true;
    dirLight.shadow.mapSize.set(1024, 1024);
    scene.add(dirLight);

    const fillLight = new THREE.DirectionalLight(0xffffff, 0.2);
    fillLight.position.set(-1000, 500, -500);
    scene.add(fillLight);

    const grid = new THREE.GridHelper(3000, 20, 0x94a3b8, 0xe2e8f0);
    scene.add(grid);

    animate();
  }

  function animate() {
    animationId = requestAnimationFrame(animate);
    if (controls) controls.update();
    if (renderer && scene && camera) {
      renderer.render(scene, camera);
    }
  }

  const typeColors = {
    side: 0x3b82f6, layer: 0xf59e0b, door: 0x10b981,
    back: 0x94a3b8, top: 0x8b5cf6, bottom: 0x8b5cf6
  };
  const textureLoader = new THREE.TextureLoader();
  textureLoader.setCrossOrigin('anonymous');
  const textureCache = new Map();

  function getTexture(url) {
    if (!url) return null;
    if (textureCache.has(url)) return textureCache.get(url);
    const texture = textureLoader.load(url, () => {
      if (renderer && scene && camera) renderer.render(scene, camera);
    });
    texture.colorSpace = THREE.SRGBColorSpace;
    texture.wrapS = THREE.RepeatWrapping;
    texture.wrapT = THREE.RepeatWrapping;
    textureCache.set(url, texture);
    return texture;
  }

  function getBoardGeometryArgs(board) {
    const thickness = board.thickness || 18;
    const designLength = board.designLength || 2200;
    const designWidth = board.designWidth || 600;

    switch (board.type) {
      case 'side':
        return [thickness, designLength, designWidth];
      case 'back':
        return [designLength, designWidth, thickness];
      case 'door':
        return [designWidth, designLength, thickness];
      case 'top':
      case 'bottom':
      case 'layer':
      default:
        return [designLength, thickness, designWidth];
    }
  }

  function buildCabinet(boards) {
    if (!scene || !camera || !controls) return;
    clearScene();

    boards.forEach(board => {
      const pos = board.position || { x: 0, y: 0, z: 0 };
      const rot = board.rotation || { x: 0, y: 0, z: 0 };
      const geo = new THREE.BoxGeometry(...getBoardGeometryArgs(board));
      const texture = getTexture(board.textureUrl);
      const mat = new THREE.MeshStandardMaterial({
        color: texture ? 0xffffff : board.appearanceColor || typeColors[board.type] || 0x94a3b8,
        map: texture,
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

    const box = new THREE.Box3();
    Array.from(boardMeshes.value.values()).forEach(mesh => box.expandByObject(mesh));
    if (box.isEmpty()) return;
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

  function resetView() {
    if (!camera || !controls || boardMeshes.value.size === 0) return;
    const box = new THREE.Box3();
    Array.from(boardMeshes.value.values()).forEach(mesh => box.expandByObject(mesh));
    if (box.isEmpty()) return;
    const center = new THREE.Vector3();
    box.getCenter(center);
    const size = new THREE.Vector3();
    box.getSize(size);
    const maxDim = Math.max(size.x, size.y, size.z, 600);
    const dist = maxDim * 2.5;
    controls.target.copy(center);
    camera.position.set(center.x + dist * 0.6, center.y + dist * 0.4, center.z + dist * 0.6);
    controls.update();
  }

  function clearScene() {
    if (!scene) return;
    boardMeshes.value.forEach(m => {
      scene.remove(m);
      m.geometry?.dispose();
      m.material?.dispose();
    });
    boardMeshes.value.clear();
    edgeLines.value.forEach(l => {
      scene.remove(l);
      l.geometry?.dispose();
      l.material?.dispose();
    });
    edgeLines.value.clear();
    if (highlightLine) {
      scene.remove(highlightLine);
      highlightLine.geometry?.dispose();
      highlightLine.material?.dispose();
      highlightLine = null;
    }
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

  function getDropPoint(event, options = {}) {
    if (!canvasRef.value || !camera) return null;
    const rect = canvasRef.value.getBoundingClientRect();
    const mouse = new THREE.Vector2(
      ((event.clientX - rect.left) / rect.width) * 2 - 1,
      -((event.clientY - rect.top) / rect.height) * 2 + 1
    );
    const y = Number(options.y) || 0;
    const snapSize = Number(options.snapSize) || 50;
    const plane = new THREE.Plane(new THREE.Vector3(0, 1, 0), -y);
    const point = new THREE.Vector3();
    raycaster.setFromCamera(mouse, camera);
    const hit = raycaster.ray.intersectPlane(plane, point);
    if (!hit) return null;
    return {
      x: Math.round(point.x / snapSize) * snapSize,
      y,
      z: Math.round(point.z / snapSize) * snapSize
    };
  }

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
    animationId = null;
    clearScene();
    if (controls) { controls.dispose(); controls = null; }
    if (renderer) { renderer.dispose(); renderer = null; }
    textureCache.forEach(texture => texture.dispose());
    textureCache.clear();
    scene = null;
    camera = null;
  }

  return {
    canvasRef,
    init,
    buildCabinet,
    clearScene,
    highlight,
    removeHighlight,
    onClick,
    getDropPoint,
    resetView,
    resize,
    dispose
  };
}
