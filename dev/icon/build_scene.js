// Veinminer icon v2 scene + animation. Run inside Blockbench (free format project):
//   eval(require('fs').readFileSync('<this file>', 'utf8')); VM.loadTextures()   // then, in a later call:
//   VM.build(); VM.animate(); VM.camera()                                          // then:
//   VM.render(first, last)                                                         // 1600px frames -> frames/
// Screen space: the `screen` group is tilted to face the orthographic camera, so inside it x = right, y = up, z = toward camera.
var VM = (function () {
  const fs = require('fs');
  const DIR = '/home/emppu/Projects/Minecraft Datapacks/Veinminer/dev/icon/';
  const TEX = DIR + 'sprites/';
  const FPS = 25, DT = 1 / FPS, LEN = 2.4;
  const CAM_POS = [0, 60, 104], CAM_TARGET = [0, 16, 0], CAM_PAN = [2.7, 8.1, 0], CAM_ZOOM = 0.45;
  const PITCH = -Math.atan2(CAM_POS[1] - CAM_TARGET[1], CAM_POS[2] - CAM_TARGET[2]) * 180 / Math.PI;
  const O = new THREE.Vector3(...CAM_TARGET);
  const RAD = Math.PI / 180;
  const Z_ITEMS = 40, Z_FX = 50, Z_PICK = 30, Z_DEBRIS = 35;

  // chain order: struck block first
  const BLOCKS = [[1, 0, 0], [0, 0, 0], [0, 0, -1], [0, 1, -1]];

  const T = {
    windStart: 0.20, cocked: 0.44, mid: 0.48, impact: 0.52, stopEnd: 0.60,
    pops: [0.60, 0.70, 0.78, 0.86],
    pickup: [1.44, 1.50, 1.56, 1.62],
    respawn: [1.64, 1.70, 1.76, 1.82],
    glint: 2.12,
  };

  const q = t => Math.round(t * FPS) / FPS;
  const worldToScreen = w => new THREE.Vector3(...w).sub(O).applyEuler(new THREE.Euler(-PITCH * RAD, 0, 0)).add(O);
  const veinToWorld = a => new THREE.Vector3(...a).applyEuler(new THREE.Euler(0, 45 * RAD, 0));
  const blockCentre = b => worldToScreen(veinToWorld([b[0] * 16, b[1] * 16 + 8, b[2] * 16]).toArray());

  const tex = {};
  function loadTextures() {
    Texture.all.slice().forEach(t => t.remove(true));
    for (const f of fs.readdirSync(TEX).filter(f => f.endsWith('.png') && !/^(bg|banner)_/.test(f))) {
      const url = 'data:image/png;base64,' + fs.readFileSync(TEX + f).toString('base64');
      tex[f.slice(0, -4)] = new Texture({ name: f }).fromDataURL(url).add(false);
    }
    return Object.keys(tex).join(',');
  }
  function ensureTex() {
    if (!Object.keys(tex).length) Texture.all.forEach(t => { tex[t.name.replace('.png', '')] = t; });
  }
  function pixels(name) {
    const t = tex[name], img = t.img;
    const cv = document.createElement('canvas');
    cv.width = img.naturalWidth; cv.height = img.naturalHeight;
    const ctx = cv.getContext('2d'); ctx.drawImage(img, 0, 0);
    const d = ctx.getImageData(0, 0, cv.width, cv.height).data, out = [];
    for (let y = 0; y < cv.height; y++) for (let x = 0; x < cv.width; x++) if (d[(y * cv.width + x) * 4 + 3] > 127) out.push([x, y]);
    return out;
  }

  function group(name, origin, parent, rotation) {
    const g = new Group({ name, origin, rotation: rotation || [0, 0, 0] });
    g.addTo(parent); g.init();
    return g;
  }
  const FACES = ['north', 'south', 'east', 'west', 'up', 'down'];
  function cube(name, from, to, parent, faceTex, opts) {
    const c = new Cube(Object.assign({ name, from, to, box_uv: false }, opts || {}));
    c.addTo(parent); c.init();
    for (const f of FACES) {
      const spec = faceTex[f] || faceTex.all;
      if (spec) c.faces[f].extend({ texture: tex[spec[0]].uuid, uv: spec[1] });
      else c.faces[f].extend({ texture: null });
    }
    return c;
  }
  function plane(name, centre, size, parent, texName, uvSize, mirror) {
    const [x, y, z] = centre, h = size / 2;
    const uv = mirror ? [uvSize, 0, 0, uvSize] : [0, 0, uvSize, uvSize];
    return cube(name, [x - h, y - h, z], [x + h, y + h, z], parent, { south: [texName, uv] });
  }
  // one cube per opaque sprite pixel; (px,py) = sprite pixel placed at `at`, pixel size s
  function extrude(prefix, texName, pivotPx, at, s, parent) {
    for (const [x, y] of pixels(texName)) {
      const x0 = at[0] + (x - pivotPx[0]) * s, y0 = at[1] + (pivotPx[1] - y - 1) * s;
      cube(prefix, [x0, y0, at[2] - s / 2], [x0 + s, y0 + s, at[2] + s / 2], parent, { all: [texName, [x, y, x + 1, y + 1]] });
    }
  }

  const G = {};
  function build() {
    ensureTex();
    Animation.all.slice().forEach(a => a.remove(false));
    Outliner.root.slice().forEach(n => n.remove(false));

    G.vein = group('vein', [0, 0, 0], undefined, [0, 45, 0]);
    BLOCKS.forEach((b, i) => {
      const [cx, cy, cz] = [b[0] * 16, b[1] * 16, b[2] * 16];
      const g = G['ore_' + i] = group('ore_' + i, [cx, cy, cz], G.vein);
      cube('block_' + i, [cx - 8, cy, cz - 8], [cx + 8, cy + 16, cz + 8], g, {
        up: ['ore_top', [0, 0, 16, 16]], west: ['ore_side_left', [0, 0, 16, 16]], south: ['ore_side_right', [0, 0, 16, 16]],
        north: ['ore_side_right', [0, 0, 16, 16]], east: ['ore_side_left', [0, 0, 16, 16]], down: ['ore_side_right', [0, 0, 16, 16]],
      });
      const sh = G['flash_' + i] = group('flash_' + i, [cx, cy + 8, cz], g);
      cube('flash_cube_' + i, [cx - 8, cy, cz - 8], [cx + 8, cy + 16, cz + 8], sh, { all: ['hit_flash', [0, 0, 16, 16]] }, { inflate: 0.35 });
    });

    G.screen = group('screen', O.toArray(), undefined, [PITCH, 0, 0]);
    const L = layout();

    G.pickaxe = group('pickaxe', [L.grip.x, L.grip.y, Z_PICK], G.screen);
    extrude('pick', 'pickaxe_item', PICK_PIVOT, [L.grip.x, L.grip.y, Z_PICK], PICK_PX, G.pickaxe);
    G.smear = group('smear', [L.grip.x, L.grip.y, Z_PICK - 2], G.screen);
    const S = SMEAR_R * 16 / 15.6;
    cube('smear_plane', [L.grip.x - S, L.grip.y, Z_PICK - 2], [L.grip.x, L.grip.y + S, Z_PICK - 2], G.smear, { south: ['fx_smear', [16, 0, 0, 16]] });

    BLOCKS.forEach((b, i) => {
      const c = L.centres[i], s = L.slots[i];
      const it = G['item_' + i] = group('item_' + i, [s.x, s.y, Z_ITEMS + i], G.screen);
      const sp = G['item_spin_' + i] = group('item_spin_' + i, [s.x, s.y + ITEM_H / 2, Z_ITEMS + i], it);
      extrude('gold', 'raw_gold_item', ITEM_PIVOT, [s.x, s.y, Z_ITEMS + i], ITEM_PX, sp);
      for (let k = 0; k < DEBRIS.length; k++) {
        const d = G[`debris_${i}_${k}`] = group(`debris_${i}_${k}`, [c.x, c.y, Z_DEBRIS + k], G.screen);
        const h = DEBRIS[k].size / 2, uv = DEBRIS[k].uv;
        cube('chunk', [c.x - h, c.y - h, Z_DEBRIS + k - h], [c.x + h, c.y + h, Z_DEBRIS + k + h], d, { all: [DEBRIS[k].tex, uv] });
      }
      G['puff_' + i] = group('puff_' + i, [c.x, c.y, Z_FX + i], G.screen);
      plane('puff_plane', [c.x, c.y, Z_FX + i], 20, G['puff_' + i], 'fx_puff', 16);
      G['pling_' + i] = group('pling_' + i, [s.x, s.y + ITEM_H + 2, Z_FX + 5], G.screen);
      plane('pling_plane', [s.x, s.y + ITEM_H + 2, Z_FX + 5], 8, G['pling_' + i], 'fx_spark', 16);
    });

    G.star = group('star', [L.tip.x, L.tip.y, Z_FX + 8], G.screen);
    plane('star_plane', [L.tip.x, L.tip.y, Z_FX + 8], 18, G.star, 'fx_star', 16);
    G.ring = group('ring', [L.tip.x, L.tip.y, Z_FX + 7], G.screen);
    plane('ring_plane', [L.tip.x, L.tip.y, Z_FX + 7], 22, G.ring, 'fx_ring', 16);
    SPARKS.forEach((a, k) => {
      G['spark_' + k] = group('spark_' + k, [L.tip.x, L.tip.y, Z_FX + 9], G.screen);
      plane('spark_plane', [L.tip.x, L.tip.y, Z_FX + 9], 6, G['spark_' + k], 'fx_spark', 16);
    });
    G.glint = group('glint', [L.glint.x, L.glint.y, Z_FX + 10], G.screen);
    plane('glint_plane', [L.glint.x, L.glint.y, Z_FX + 10], 8, G.glint, 'fx_spark', 16);

    Canvas.updateAll();
    return Outliner.elements.length;
  }

  // ---- geometry (screen space) ----
  const PICK_PX = 1.6, PICK_PIVOT = [3.5, 12.5], PICK_TIP = [5.5, 3.5], TRAIL_DIR = 5.7, SMEAR_R = 20;
  const ITEM_PX = 0.75, ITEM_PIVOT = [8, 15], ITEM_H = 13 * 0.75;
  const DEBRIS = [
    { size: 3.2, tex: 'chunks', uv: [0, 0, 4, 4] },
    { size: 2.6, tex: 'chunks', uv: [4, 0, 8, 4] },
    { size: 2.2, tex: 'chunks', uv: [8, 0, 12, 4] },
  ];
  const SPARKS = [20, 75, 130, 170];
  const P = {
    impactRot: 140, restRot: 55, cockedRot: 20, rest: [-3, -1, 0],
    slotX: [16.5, 5.5, -5.5, -16.5], rowY: -1,
    apex: [10, 12, 11, 8], flight: [8, 9, 9, 8],
    bounce: [[4, 4], [1.2, 3]],
  };
  function rot2(v, deg) {
    const a = deg * RAD;
    return [v[0] * Math.cos(a) - v[1] * Math.sin(a), v[0] * Math.sin(a) + v[1] * Math.cos(a)];
  }
  function layout() {
    const centres = BLOCKS.map(blockCentre);
    const b0 = BLOCKS[0];
    const top = worldToScreen(veinToWorld([b0[0] * 16 + 1, b0[1] * 16 + 16, b0[2] * 16 + 3]).toArray());
    const tipOff = rot2([(PICK_TIP[0] - PICK_PIVOT[0]) * PICK_PX, (PICK_PIVOT[1] - PICK_TIP[1]) * PICK_PX], P.impactRot);
    const grip = { x: top.x - tipOff[0], y: top.y - tipOff[1] };
    const rowY = worldToScreen([0, 0, 0]).y + P.rowY;
    const slots = P.slotX.map(x => ({ x, y: rowY }));
    const b3 = BLOCKS[3];
    const glint = worldToScreen(veinToWorld([b3[0] * 16 - 4, b3[1] * 16 + 16, b3[2] * 16 + 4]).toArray());
    return { centres, grip, tip: top, slots, glint };
  }

  // ---- animation ----
  let A = null;
  function K(g, ch, t, v, interp) {
    const [x, y, z] = typeof v === 'number' ? [v, v, v] : v;
    A.getBoneAnimator(g).addKeyframe({ channel: ch, time: q(t), interpolation: interp || 'linear', data_points: [{ x, y, z }] });
  }
  const track = (g, ch, keys, interp) => keys.forEach(([t, v, i]) => K(g, ch, t, v, i || interp));

  function ensureG() {
    if (!Object.keys(G).length) Group.all.forEach(g => { G[g.name] = g; });
  }
  function animate() {
    ensureTex(); ensureG();
    Animation.all.slice().forEach(a => a.remove(false));
    A = new Animation({ name: 'icon_loop', length: LEN, loop: 'loop', snapping: FPS }).add(false);
    A.select();
    const L = layout(), warnings = [];
    const R = P.rest;
    const add = (a, b) => a.map((v, i) => v + b[i]);

    // pickaxe: still at rest (a per-frame idle bob costs ~15 KiB of GIF) -> anticipation dip -> cock back -> 2-frame whip -> hit-stop -> recoil wobble
    const I = T.impact, W = T.windStart, rr = P.restRot, z = r => [0, 0, r];
    track(G.pickaxe, 'rotation', [
      [0, z(rr), 'catmullrom'], [W, z(rr), 'catmullrom'],
      [W + 0.08, z(rr + 6), 'catmullrom'], [W + 0.16, z(P.cockedRot + 14), 'catmullrom'], [T.cocked, z(P.cockedRot)],
      [T.mid, z(80)], [I, z(P.impactRot)], [I + 0.04, z(P.impactRot + 3)], [T.stopEnd, z(P.impactRot + 3)],
      [T.stopEnd + 0.04, z(124), 'catmullrom'], [T.stopEnd + 0.08, z(100), 'catmullrom'], [T.stopEnd + 0.16, z(rr - 13), 'catmullrom'],
      [T.stopEnd + 0.26, z(rr + 11), 'catmullrom'], [T.stopEnd + 0.36, z(rr - 5), 'catmullrom'], [T.stopEnd + 0.48, z(rr + 2), 'catmullrom'],
      [T.stopEnd + 0.60, z(rr), 'catmullrom'], [LEN, z(rr), 'catmullrom'],
    ]);
    track(G.pickaxe, 'position', [
      [0, R, 'catmullrom'], [W, R, 'catmullrom'],
      [W + 0.08, add(R, [1, -1, 0]), 'catmullrom'], [W + 0.16, [-5, 4, 0], 'catmullrom'], [T.cocked, [-6, 6, 0]],
      [T.mid, [-3, 3, 0]], [I, [0, 0, 0]], [I + 0.04, [0, -0.6, 0]], [T.stopEnd, [0, -0.6, 0]],
      [T.stopEnd + 0.04, [-2, 3, 0], 'catmullrom'], [T.stopEnd + 0.12, add(R, [0, 4, 0]), 'catmullrom'], [T.stopEnd + 0.24, R, 'catmullrom'],
      [LEN, R, 'catmullrom'],
    ]);
    track(G.pickaxe, 'scale', [
      [0, 1], [W, 1], [W + 0.08, [1.05, 0.95, 1]], [T.cocked, [0.92, 1.12, 1]], [T.mid, [1.1, 0.94, 1]],
      [I, [1.06, 0.95, 1]], [T.stopEnd, [1.06, 0.95, 1]], [T.stopEnd + 0.04, [0.95, 1.06, 1]], [T.stopEnd + 0.12, 1], [LEN, 1],
    ]);

    // smear on the two strike frames: thick end just behind the head's trailing (leg) tip, arc covering the swept path
    const smearRot = th => TRAIL_DIR + th - 2 - 180;
    track(G.smear, 'rotation', [[T.mid, z(smearRot(80)), 'step'], [I, z(smearRot(P.impactRot)), 'step']]);
    track(G.smear, 'position', [[T.mid, [-3, 3, 0], 'step'], [I, [0, 0, 0], 'step']]);
    track(G.smear, 'scale', [[0, 0, 'step'], [T.mid, 1, 'step'], [I, 0.85, 'step'], [I + 0.04, 0, 'step']]);

    // impact FX
    track(G.star, 'scale', [[0, 0], [I - 0.04, 0], [I, 1.35], [I + 0.04, 1.0], [I + 0.08, 0.5], [I + 0.12, 0]]);
    track(G.star, 'rotation', [[I, z(0)], [I + 0.12, z(30)]]);
    track(G.ring, 'scale', [[0, 0], [I - 0.04, 0], [I, 0.45], [I + 0.04, 0.9], [I + 0.08, 1.3], [I + 0.12, 0]]);
    SPARKS.forEach((a, k) => {
      const keys = [[0, 0], [I - 0.04, 0]], pos = [];
      for (let f = 0; f <= 4; f++) {
        const t = I + f * DT, d = 17 * (1 - Math.pow(1 - f / 4, 2));
        keys.push([t, f === 4 ? 0 : 1 - f * 0.2]);
        pos.push([t, [Math.cos(a * RAD) * d, Math.sin(a * RAD) * d, 0]]);
      }
      track(G['spark_' + k], 'scale', keys);
      track(G['spark_' + k], 'position', pos);
    });
    track(G.vein, 'position', [[I - 0.04, [0, 0, 0]], [I, [1.2, -0.6, 0]], [I + 0.04, [-1.0, 0, 0]], [I + 0.08, [0.6, 0, 0]], [I + 0.12, [0, 0, 0]]]);

    // blocks: squash on hit, cascade pops, elastic respawn
    BLOCKS.forEach((b, i) => {
      const tp = T.pops[i], tr = T.respawn[i];
      const ore = G['ore_' + i];
      const pre = i === 0
        ? [[T.impact - 0.04, 1], [T.impact, [1.2, 0.7, 1.2]], [T.impact + 0.04, [1.22, 0.66, 1.22]]]
        : [[tp - 0.08, 1], [tp - 0.04, [1.15, 0.82, 1.15]]];
      track(ore, 'scale', [
        [0, 1], ...pre, [tp, [0.86, 1.28, 0.86]], [tp + 0.04, 0],
        [tr, 0], [tr + 0.04, [0.8, 1.3, 0.8]], [tr + 0.08, [1.25, 0.78, 1.25]], [tr + 0.12, [0.92, 1.1, 0.92]],
        [tr + 0.16, [1.04, 0.97, 1.04]], [tr + 0.24, 1], [LEN, 1],
      ]);
      const flashAt = i === 0 ? T.impact : tp - 0.04;
      track(G['flash_' + i], 'scale', [[0, 0, 'step'], [flashAt, 1, 'step'], [flashAt + 0.04, 0, 'step']]);

      const t0 = tp + 0.04, c = L.centres[i], s = L.slots[i];
      track(G['puff_' + i], 'scale', [[0, 0], [tp, 0], [t0, 0.7], [t0 + 0.04, 1.25], [t0 + 0.08, 1.1], [t0 + 0.12, 0.6], [t0 + 0.16, 0]]);
      track(G['puff_' + i], 'position', [[t0, [0, 0, 0]], [t0 + 0.16, [0, 4, 0]]]);

      DEBRIS.forEach((d, k) => {
        const g = G[`debris_${i}_${k}`];
        const ang = (i * 47 + k * 120 + 35) % 360, sp = 70 + 25 * k, grav = 900;
        const vx = Math.cos(ang * RAD) * sp, vy = Math.abs(Math.sin(ang * RAD)) * sp + 60;
        const pos = [], rot = [];
        for (let f = 0; f <= 9; f++) {
          const t = f * DT;
          pos.push([t0 + t, [vx * t, vy * t - grav * t * t / 2, 0]]);
          rot.push([t0 + t, [f * 40, 0, f * (k % 2 ? 55 : -55)]]);
        }
        track(g, 'position', pos);
        track(g, 'rotation', rot);
        track(g, 'scale', [[0, 0], [tp, 0], [t0, 1], [t0 + 0.24, 1], [t0 + 0.32, 0.5], [t0 + 0.36, 0]]);
      });

      // raw gold: arc from the block to its slot, spin, land, two bounces, pickup
      const it = G['item_' + i], spin = G['item_spin_' + i];
      const start = [c.x - s.x, c.y - ITEM_H / 2 - s.y], H = P.apex[i], n = P.flight[i], Tf = n * DT;
      const dy = -start[1];
      const gF = Math.pow((Math.sqrt(2 * H) + Math.sqrt(2 * (H - dy))) / Tf, 2), vy = Math.sqrt(2 * gF * H);
      const vx = -start[0] / Tf;
      const pos = [[0, [start[0], start[1], 0], 'step'], [tp, [start[0], start[1], 0]]];
      const rotK = [], dir = vx >= 0 ? -1 : 1;
      for (let f = 0; f <= n; f++) {
        const t = f * DT;
        pos.push([t0 + t, [start[0] + vx * t, start[1] + vy * t - gF * t * t / 2, 0]]);
        rotK.push([t0 + t, [0, 0, dir * 360 * (1 - f / n)]]);
      }
      let tb = t0 + Tf;
      const sc = [[0, 0], [tp, 0], [t0, 0.6], [t0 + 0.04, 1.2], [t0 + 0.08, 1], [tb - 0.04, [0.9, 1.1, 1]], [tb, [1.35, 0.65, 1]]];
      P.bounce.forEach(([h, m], j) => {
        for (let f = 1; f <= m; f++) {
          const u = f / m;
          pos.push([tb + f * DT, [0, 4 * h * u * (1 - u), 0]]);
        }
        sc.push([tb + DT, j ? [0.95, 1.06, 1] : [0.85, 1.2, 1]]);
        if (m > 2) sc.push([tb + 2 * DT, 1]);
        tb += m * DT;
        sc.push([tb, j ? [1.1, 0.9, 1] : [1.22, 0.8, 1]]);
      });
      sc.push([tb + DT, [0.96, 1.04, 1]], [tb + 2 * DT, 1]);
      const tu = T.pickup[i];
      if (tb + 2 * DT > tu + 1e-6) warnings.push(`item ${i} still bouncing at pickup (${tb.toFixed(2)} > ${tu})`);
      pos.push([tu, [0, 0, 0]], [tu + 0.04, [0, 2.5, 0]], [tu + 0.08, [0, 5, 0]], [tu + 0.12, [0, 6, 0]]);
      sc.push([tu, [0.9, 1.15, 1]], [tu + 0.04, 1.3], [tu + 0.08, 0.6], [tu + 0.12, 0]);
      track(it, 'position', pos);
      track(it, 'scale', sc);
      track(spin, 'rotation', rotK);
      track(G['pling_' + i], 'scale', [[0, 0], [tu, 0], [tu + 0.04, 0.5], [tu + 0.08, 1.3], [tu + 0.12, 0.9], [tu + 0.16, 0]]);
      track(G['pling_' + i], 'rotation', [[tu, [0, 0, 0]], [tu + 0.16, [0, 0, 45]]]);
    });

    track(G.glint, 'scale', [[0, 0], [T.glint, 0], [T.glint + 0.04, 0.6], [T.glint + 0.08, 1.2], [T.glint + 0.12, 0.7], [T.glint + 0.16, 0]]);
    Animator.preview();
    return warnings.length ? warnings.join('; ') : layoutInfo();
  }
  function layoutInfo() {
    const L = layout(), r = v => Math.round(v * 10) / 10;
    return JSON.stringify({ grip: [r(L.grip.x), r(L.grip.y)], tip: [r(L.tip.x), r(L.tip.y)], slots: L.slots.map(s => [r(s.x), r(s.y)]), centres: L.centres.map(c => [r(c.x), r(c.y)]) });
  }

  // ---- camera + render ----
  function camera(zoom) {
    const p = Preview.selected;
    p.setProjectionMode(true);
    const pos = CAM_POS.map((v, i) => v + CAM_PAN[i]), tgt = CAM_TARGET.map((v, i) => v + CAM_PAN[i]);
    p.camera.position.set(...pos);
    p.controls.target.set(...tgt);
    p.camera.lookAt(...tgt);
    p.camera.zoom = zoom || CAM_ZOOM; p.camera.updateProjectionMatrix();
    p.controls.update();
  }
  function setTime(t) {
    Timeline.setTime(t);
    Animator.preview();
  }
  function render(first, last, res, dir) {
    res = res || 1600;
    dir = dir || DIR + 'frames/';
    if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
    const shot = f => new Promise(done => {
      setTime(f * DT);
      Screencam.advancedScreenshot(Preview.selected, { angle_preset: 'view', resolution: [res, res], anti_aliasing: 'none', shading: false }, url => {
        fs.writeFileSync(dir + 'frame_' + String(f).padStart(3, '0') + '.png', Buffer.from(url.split(',')[1], 'base64'));
        done();
      });
    });
    return (async () => { for (let f = first; f <= last; f++) await shot(f); return `rendered ${first}..${last}`; })();
  }
  function scaleSweep() {
    const bad = [];
    for (let f = 0; f <= Math.round(LEN * FPS); f++) {
      setTime(f * DT);
      Group.all.forEach(g => { const s = g.mesh.scale; if (s.x < 0 || s.y < 0 || s.z < 0) bad.push(g.name + '@' + f); });
    }
    return bad.length ? bad.join(',') : 'no negative scale';
  }

  return { FPS, DT, LEN, T, P, PITCH, BLOCKS, G, tex, loadTextures, build, animate, camera, render, setTime, scaleSweep, layoutInfo, pixels, worldToScreen, veinToWorld, blockCentre, q };
})();
