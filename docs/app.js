// Miogram Official Portal App
// 1. GitHub Releases API Client
// 2. Interactive Canvas Pixel Badge Renderer with Bloom & Starlight Particles

const REPO_OWNER = "fuckramochka";
const REPO_NAME = "miogram";
const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/releases/latest`;
const FALLBACK_URL = `https://github.com/${REPO_OWNER}/${REPO_NAME}/releases/latest`;

// Fetch Latest Release
async function fetchLatestRelease() {
  const releaseTagEl = document.getElementById("releaseTag");
  const releaseDateEl = document.getElementById("releaseDate");
  const releaseNotesEl = document.getElementById("releaseNotes");
  const primaryApkLink = document.getElementById("primaryApkLink");
  const heroDownloadBtn = document.getElementById("hero-download-btn");
  const primaryApkText = document.getElementById("primaryApkText");

  try {
    const res = await fetch(API_URL);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();

    const tag = data.tag_name || "v1.0";
    const dateStr = data.published_at ? new Date(data.published_at).toLocaleDateString() : "";
    const notes = data.body || "Latest stable release with 10 community badges and zero-trust vault.";

    releaseTagEl.textContent = tag;
    releaseDateEl.textContent = dateStr ? `• Released ${dateStr}` : "";
    releaseNotesEl.textContent = notes;

    // Search for apk asset
    let apkUrl = FALLBACK_URL;
    let apkSize = "";
    if (data.assets && data.assets.length > 0) {
      const apkAsset = data.assets.find(a => a.name.endsWith(".apk"));
      if (apkAsset) {
        apkUrl = apkAsset.browser_download_url;
        apkSize = ` (${(apkAsset.size / (1024 * 1024)).toFixed(1)} MB)`;
      }
    }

    primaryApkLink.href = apkUrl;
    heroDownloadBtn.href = apkUrl;
    primaryApkText.textContent = `Download Miogram ${tag}${apkSize}`;
  } catch (err) {
    console.warn("Could not fetch release info, using fallback:", err);
    releaseTagEl.textContent = "Latest Release";
    releaseNotesEl.textContent = "Download the latest APK directly from the GitHub releases page.";
    primaryApkLink.href = FALLBACK_URL;
    heroDownloadBtn.href = FALLBACK_URL;
  }
}

// -------------------------------------------------------------
// Interactive 10 Badges Canvas Renderer
// -------------------------------------------------------------
const canvas = document.getElementById("badgeCanvas");
const ctx = canvas.getContext("2d");

let currentBadgeId = "original";
let currentBadgeNum = "01";
let currentBadgeName = "ORIGINAL";
let currentBadgeLore = "Canonical Miogram winged heart with antenna visor, obsidian core, electric cyan glowing contour, and pink feather tips. The inaugural distinction of the ecosystem.";

const BADGE_CONFIGS = {
  original: {
    accent: "#00F0FF",
    glow: "rgba(0, 240, 255, 0.4)",
    title: "Classical Cyber Heart",
    name: "01 — ORIGINAL"
  },
  pink: {
    accent: "#FF2A93",
    glow: "rgba(255, 42, 147, 0.4)",
    title: "Neon Cyber Pink",
    name: "02 — PINK"
  },
  cyan: {
    accent: "#00E5FF",
    glow: "rgba(0, 229, 255, 0.4)",
    title: "Electric Sky Blue",
    name: "03 — CYAN"
  },
  dark: {
    accent: "#C77DFF",
    glow: "rgba(157, 78, 221, 0.4)",
    title: "Midnight Obsidian",
    name: "04 — DARK"
  },
  angel: {
    accent: "#FFFFFF",
    glow: "rgba(224, 170, 255, 0.35)",
    title: "Floating Halo Angel",
    name: "05 — ANGEL"
  },
  devil: {
    accent: "#FF0055",
    glow: "rgba(255, 0, 85, 0.4)",
    title: "Playful Devil Bat",
    name: "06 — DEVIL"
  },
  rainbow: {
    accent: "#FFD166",
    glow: "rgba(255, 209, 102, 0.4)",
    title: "Prismatic Spectrum",
    name: "07 — RAINBOW"
  },
  outline: {
    accent: "#00F0FF",
    glow: "rgba(0, 240, 255, 0.3)",
    title: "Minimal Cyber Wireframe",
    name: "08 — OUTLINE"
  },
  glitch: {
    accent: "#00F0FF",
    glow: "rgba(0, 240, 255, 0.35)",
    title: "Split Chromatic Aberration",
    name: "09 — GLITCH"
  },
  premium: {
    accent: "#FFD700",
    glow: "rgba(255, 215, 0, 0.45)",
    title: "Royal Golden Crown",
    name: "10 — PREMIUM"
  }
};

// 6 Dynamic Flying Micro-Particles: {x, speed, sway, amp, isCross, offset}
const DYNAMIC_PARTICLES = [
  { x: 2.5,  speed: 0.00035, sway: 0.08, amp: 1.2, isCross: true,  offset: 0.05 },
  { x: 24.5, speed: 0.00042, sway: 0.07, amp: 1.2, isCross: true,  offset: 0.45 },
  { x: 3.5,  speed: 0.00028, sway: 0.09, amp: 1.0, isCross: false, offset: 0.70 },
  { x: 23.5, speed: 0.00038, sway: 0.08, amp: 1.0, isCross: false, offset: 0.25 },
  { x: 13.5, speed: 0.00045, sway: 0.06, amp: 1.4, isCross: false, offset: 0.85 },
  { x: 13.5, speed: 0.00032, sway: 0.07, amp: 1.2, isCross: true,  offset: 0.35 }
];

let animStart = performance.now();

function renderLoop(time) {
  const elapsed = time - animStart;
  const cycle = (elapsed % 2200) / 2200;
  const angle = cycle * 2 * Math.PI;

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const GRID_W = 28;
  const GRID_H = 22;
  const px = 8.5;
  const py = 8.5;

  const offsetX = (canvas.width - GRID_W * px) / 2;
  const offsetY = (canvas.height - GRID_H * py) / 2;

  // Floating respiration hop
  const bobY = Math.round(Math.sin(angle) * 1.0) * py;

  ctx.save();
  ctx.translate(offsetX, offsetY + bobY);

  const cfg = BADGE_CONFIGS[currentBadgeId] || BADGE_CONFIGS.original;

  // 1. Radiant Atmospheric Bloom
  const cx = 14 * px;
  const cy = 11 * py;
  const radGlow = ctx.createRadialGradient(cx, cy, 5, cx, cy, 120);
  radGlow.addColorStop(0, cfg.glow);
  radGlow.addColorStop(1, "transparent");
  ctx.fillStyle = radGlow;
  ctx.beginPath();
  ctx.arc(cx, cy, 120, 0, Math.PI * 2);
  ctx.fill();

  // 2. Render badge geometry
  drawBadgeGeometry(ctx, px, py, currentBadgeId, cfg.accent, elapsed);

  // 3. Dynamic Flying Micro-Particles
  drawDynamicSparkles(ctx, px, py, elapsed, cfg.accent);

  ctx.restore();
  requestAnimationFrame(renderLoop);
}

function drawBadgeGeometry(c, px, py, id, accent, elapsed) {
  // Common Heart
  function drawHeart(fill, stroke) {
    c.fillStyle = fill;
    c.fillRect(10 * px, 7 * py, 3 * px, 1 * py);
    c.fillRect(15 * px, 7 * py, 3 * px, 1 * py);
    c.fillRect(9 * px, 8 * py, 10 * px, 2 * py);
    c.fillRect(8 * px, 10 * py, 12 * px, 2 * py);
    c.fillRect(9 * px, 12 * py, 10 * px, 1 * py);
    c.fillRect(10 * px, 13 * py, 8 * px, 1 * py);
    c.fillRect(11 * px, 14 * py, 6 * px, 1 * py);
    c.fillRect(12 * px, 15 * py, 4 * px, 1 * py);
    c.fillRect(13 * px, 16 * py, 2 * px, 1 * py);

    // Glowing Contour (bottom colored pixel removed for clean natural heart tip)
    c.fillStyle = stroke;
    c.fillRect(10 * px, 6 * py, 3 * px, 1 * py);
    c.fillRect(15 * px, 6 * py, 3 * px, 1 * py);
    c.fillRect(8 * px, 8 * py, 1 * px, 4 * py);
    c.fillRect(19 * px, 8 * py, 1 * px, 4 * py);
  }

  function drawEyes(eyeColor) {
    c.fillStyle = eyeColor;
    c.fillRect(11 * px, 9.5 * py, 1.5 * px, 1.5 * py);
    c.fillRect(15.5 * px, 9.5 * py, 1.5 * px, 1.5 * py);
  }

  function drawWings(fill, tip) {
    c.fillStyle = fill;
    // Left Wing
    c.fillRect(4 * px, 5 * py, 5 * px, 1 * py);
    c.fillRect(3 * px, 6 * py, 7 * px, 1 * py);
    c.fillRect(2 * px, 7 * py, 9 * px, 1 * py);
    c.fillRect(1 * px, 8 * py, 10 * px, 1 * py);
    c.fillRect(2 * px, 9 * py, 9 * px, 1 * py);
    c.fillRect(3 * px, 10 * py, 7 * px, 1 * py);
    c.fillRect(4 * px, 11 * py, 5 * px, 1 * py);
    c.fillRect(6 * px, 12 * py, 3 * px, 1 * py);

    // Right Wing
    c.fillRect(19 * px, 5 * py, 5 * px, 1 * py);
    c.fillRect(18 * px, 6 * py, 7 * px, 1 * py);
    c.fillRect(17 * px, 7 * py, 9 * px, 1 * py);
    c.fillRect(17 * px, 8 * py, 10 * px, 1 * py);
    c.fillRect(17 * px, 9 * py, 9 * px, 1 * py);
    c.fillRect(18 * px, 10 * py, 7 * px, 1 * py);
    c.fillRect(19 * px, 11 * py, 5 * px, 1 * py);
    c.fillRect(19 * px, 12 * py, 3 * px, 1 * py);

    if (tip) {
      c.fillStyle = tip;
      c.fillRect(1 * px, 7 * py, 2 * px, 3 * py);
      c.fillRect(25 * px, 7 * py, 2 * px, 3 * py);
    }
  }

  if (id === "original") {
    // Visor
    c.fillStyle = "#00F0FF";
    c.fillRect(10 * px, 3 * py, 8 * px, 1 * py);
    c.fillRect(12 * px, 4 * py, 4 * px, 1 * py);
    drawWings("#F0FDFE", "#FF55A3");
    drawHeart("#0F141C", "#00F0FF");
    drawEyes("#FFFFFF");
  } else if (id === "pink") {
    c.fillStyle = "#FF2A93";
    c.fillRect(10 * px, 3 * py, 8 * px, 1 * py);
    drawWings("#FFF0F7", "#FF2A93");
    drawHeart("#1B0F1C", "#FF2A93");
    c.fillStyle = "#FF2A93";
    c.fillRect(11 * px, 12 * py, 6 * px, 1 * py);
    drawEyes("#FFE5F0");
  } else if (id === "cyan") {
    c.fillStyle = "#00E5FF";
    c.fillRect(10 * px, 3 * py, 8 * px, 1 * py);
    drawWings("#E0F7FA", "#00E5FF");
    drawHeart("#0A1822", "#00E5FF");
    drawEyes("#FFFFFF");
  } else if (id === "dark") {
    c.fillStyle = "#9D4EDD";
    c.fillRect(11 * px, 3 * py, 6 * px, 1 * py);
    drawWings("#1B142A", "#C77DFF");
    drawHeart("#120B20", "#9D4EDD");
    drawEyes("#E0AAFF");
  } else if (id === "angel") {
    // Halo
    c.fillStyle = "#FFFFFF";
    c.fillRect(10 * px, 1 * py, 8 * px, 1 * py);
    c.fillRect(8 * px, 2 * py, 2 * px, 1 * py);
    c.fillRect(18 * px, 2 * py, 2 * px, 1 * py);
    drawWings("#FAFAFE", "#B8C0EC");
    drawHeart("#C3BEF0", "#FFFFFF");
    drawEyes("#FFFFFF");
  } else if (id === "devil") {
    // Horns
    c.fillStyle = "#FF0055";
    c.fillRect(9 * px, 4 * py, 2 * px, 3 * py);
    c.fillRect(8 * px, 3 * py, 2 * px, 2 * py);
    c.fillRect(17 * px, 4 * py, 2 * px, 3 * py);
    c.fillRect(18 * px, 3 * py, 2 * px, 2 * py);
    drawWings("#FF3377", "#B8003D");
    drawHeart("#1C0A15", "#FF0055");
    drawEyes("#FFB3C6");
  } else if (id === "rainbow") {
    const rainbow = ["#FF3377", "#9D4EDD", "#00B4D8", "#06D6A0", "#FFD166"];
    for (let i = 0; i < 5; i++) {
      c.fillStyle = rainbow[i];
      c.fillRect(4 * px, (5 + i * 1.5) * py, 5 * px, 1.5 * py);
      c.fillRect(19 * px, (5 + i * 1.5) * py, 5 * px, 1.5 * py);
    }
    drawHeart("#10141E", "#FFD166");
    drawEyes("#FFFFFF");
  } else if (id === "outline") {
    c.strokeStyle = "#00F0FF";
    c.lineWidth = 1.5;
    c.strokeRect(4 * px, 5 * py, 5 * px, 7 * py);
    c.strokeRect(19 * px, 5 * py, 5 * px, 7 * py);
    c.strokeRect(9 * px, 7 * py, 10 * px, 9 * py);
    drawEyes("#00F0FF");
  } else if (id === "glitch") {
    // Magenta Left
    c.fillStyle = "rgba(255, 0, 85, 0.7)";
    c.fillRect(3 * px, 5 * py, 5 * px, 7 * py);
    // Cyan Right
    c.fillStyle = "rgba(0, 240, 255, 0.7)";
    c.fillRect(20 * px, 5 * py, 5 * px, 7 * py);
    drawWings("#FFFFFF", "#00F0FF");
    drawHeart("#10121C", "#00F0FF");
    drawEyes("#FFFFFF");
  } else if (id === "premium") {
    // Crown
    c.fillStyle = "#FFD700";
    c.fillRect(10 * px, 2 * py, 2 * px, 3 * py);
    c.fillRect(13 * px, 1 * py, 2 * px, 4 * py);
    c.fillRect(16 * px, 2 * py, 2 * px, 3 * py);
    c.fillRect(10 * px, 5 * py, 8 * px, 1 * py);
    drawWings("#FFE066", "#CC8800");
    drawHeart("#1B1408", "#FFD700");
    c.fillStyle = "#FFD700";
    c.fillRect(10 * px, 11 * py, 8 * px, 1 * py);
    drawEyes("#FFF5B8");
  }
}

function drawDynamicSparkles(c, px, py, now, color) {
  for (const p of DYNAMIC_PARTICLES) {
    const travel = (now * p.speed + p.offset) % 1.0;
    const y = (1.0 - travel) * 22;
    const x = p.x + Math.sin(y * p.sway + now * 0.0025) * p.amp;

    const alphaSin = Math.sin(travel * Math.PI);
    if (alphaSin <= 0.08) continue;

    c.save();
    c.globalAlpha = alphaSin;
    c.fillStyle = color;

    const sx = x * px;
    const sy = y * py;

    if (p.isCross) {
      c.fillRect(sx, sy - 1.0 * py, 1.0 * px, 3.0 * py);
      c.fillRect(sx - 1.0 * px, sy, 3.0 * px, 1.0 * py);
      c.fillStyle = "#FFFFFF";
      c.fillRect(sx, sy, 1.0 * px, 1.0 * py);
    } else {
      c.fillRect(sx - 0.5 * px, sy - 0.5 * py, 1.0 * px, 1.0 * py);
      c.fillStyle = "#FFFFFF";
      c.fillRect(sx - 0.25 * px, sy - 0.25 * py, 0.5 * px, 0.5 * py);
    }
    c.restore();
  }
}

// Selector Tile Clicks
document.querySelectorAll(".badge-tile").forEach(tile => {
  tile.addEventListener("click", () => {
    document.querySelectorAll(".badge-tile").forEach(t => t.classList.remove("active"));
    tile.classList.add("active");

    currentBadgeId = tile.dataset.id;
    currentBadgeNum = tile.dataset.num;
    currentBadgeName = tile.dataset.name;
    currentBadgeLore = tile.dataset.lore;

    const cfg = BADGE_CONFIGS[currentBadgeId] || BADGE_CONFIGS.original;
    document.getElementById("badgeNumber").textContent = cfg.name;
    document.getElementById("badgeTitle").textContent = cfg.title;
    document.getElementById("badgeDesc").textContent = currentBadgeLore;
  });
});

// Initialize on Load
window.addEventListener("DOMContentLoaded", () => {
  fetchLatestRelease();
  requestAnimationFrame(renderLoop);
});
