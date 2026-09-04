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

// 8 Dynamic Flying Micro-Particles: {x, speed, sway, amp, isCross, offset}
const DYNAMIC_PARTICLES = [
  { x: 3.5,  speed: 0.00035, sway: 0.08, amp: 1.4, isCross: true,  offset: 0.05 },
  { x: 24.5, speed: 0.00042, sway: 0.07, amp: 1.5, isCross: true,  offset: 0.35 },
  { x: 5.0,  speed: 0.00028, sway: 0.09, amp: 1.2, isCross: false, offset: 0.65 },
  { x: 23.0, speed: 0.00038, sway: 0.08, amp: 1.3, isCross: false, offset: 0.85 },
  { x: 10.0, speed: 0.00045, sway: 0.06, amp: 1.0, isCross: false, offset: 0.20 },
  { x: 18.0, speed: 0.00032, sway: 0.07, amp: 1.2, isCross: true,  offset: 0.50 },
  { x: 14.0, speed: 0.00030, sway: 0.10, amp: 1.5, isCross: false, offset: 0.75 },
  { x: 2.0,  speed: 0.00040, sway: 0.08, amp: 1.2, isCross: true,  offset: 0.90 }
];

let animStart = performance.now();

function renderLoop(time) {
  const elapsed = time - animStart;
  const cycle = (elapsed % 2400) / 2400;
  const angle = cycle * 2 * Math.PI;

  ctx.clearRect(0, 0, canvas.width, canvas.height);

  const GRID_W = 28;
  const GRID_H = 24;
  const px = 8.5;
  const py = 8.5;

  const offsetX = (canvas.width - GRID_W * px) / 2;
  const offsetY = (canvas.height - GRID_H * py) / 2;

  // Gentle floating respiration hop
  const bobY = Math.round(Math.sin(angle) * 1.0) * py;

  ctx.save();
  ctx.translate(offsetX, offsetY + bobY);

  const cfg = BADGE_CONFIGS[currentBadgeId] || BADGE_CONFIGS.original;

  // 1. Radiant Atmospheric Bloom
  const cx = 14 * px;
  const cy = 12 * py;
  const radGlow = ctx.createRadialGradient(cx, cy, 5, cx, cy, 120);
  radGlow.addColorStop(0, cfg.glow);
  radGlow.addColorStop(1, "transparent");
  ctx.fillStyle = radGlow;
  ctx.beginPath();
  ctx.arc(cx, cy, 120, 0, Math.PI * 2);
  ctx.fill();

  // 2. Dynamic Flying Micro-Particles
  drawFlyingMicroParticles(ctx, px, py, elapsed, cfg.accent, cfg.secondary || cfg.accent);

  // 3. Render badge geometry (Heart in prominent foreground)
  drawBadgeGeometry(ctx, px, py, currentBadgeId, cfg.accent, elapsed);

  ctx.restore();
  requestAnimationFrame(renderLoop);
}

function drawFlyingMicroParticles(c, px, py, now, primaryColor, secondaryColor) {
  for (let i = 0; i < DYNAMIC_PARTICLES.length; i++) {
    const p = DYNAMIC_PARTICLES[i];
    const travel = (now * p.speed + p.offset) % 1.0;
    const y = (1.0 - travel) * 23;
    const x = p.x + Math.sin(y * p.sway + now * 0.003) * p.amp;

    const alphaSin = Math.sin(travel * Math.PI);
    if (alphaSin <= 0.08) continue;

    c.save();
    c.globalAlpha = alphaSin;
    c.fillStyle = (i % 2 === 0) ? primaryColor : secondaryColor;

    const cx = x * px;
    const cy = y * py;

    if (p.isCross) {
      // Tiny 3x3 micro-cross: 1px center with 1px cardinal arms
      c.fillRect(cx - 0.35 * px, cy - 1.1 * py, 0.7 * px, 2.2 * py);
      c.fillRect(cx - 1.1 * px, cy - 0.35 * py, 2.2 * px, 0.7 * py);
      c.fillStyle = "#FFFFFF";
      c.fillRect(cx - 0.35 * px, cy - 0.35 * py, 0.7 * px, 0.7 * py);
    } else {
      // Tiny 2x2 micro-dot
      c.fillRect(cx - 0.6 * px, cy - 0.6 * py, 1.2 * px, 1.2 * py);
      c.fillStyle = "#FFFFFF";
      c.fillRect(cx - 0.3 * px, cy - 0.3 * py, 0.6 * px, 0.6 * py);
    }
    c.restore();
  }
}

function drawBadgeGeometry(c, px, py, id, accent, elapsed) {
  function drawVisor(color) {
    c.fillStyle = color;
    c.fillRect(10.0 * px, 1.5 * py, 8.0 * px, 0.6 * py);
    c.fillRect(9.0 * px, 2.5 * py, 10.0 * px, 0.6 * py);
    c.fillRect(9.0 * px, 3.5 * py, 10.0 * px, 0.6 * py);
    c.fillRect(10.0 * px, 4.5 * py, 8.0 * px, 0.6 * py);
    c.fillRect(9.0 * px, 1.5 * py, 0.6 * px, 3.6 * py);
    c.fillRect(18.4 * px, 1.5 * py, 0.6 * px, 3.6 * py);
  }

  function drawAngledWings(fill) {
    c.fillStyle = fill;
    // Left Wing (angled up-left)
    c.fillRect(5.0 * px, 3.5 * py, 4.0 * px, 1.2 * py);
    c.fillRect(3.0 * px, 4.7 * py, 6.0 * px, 1.2 * py);
    c.fillRect(1.0 * px, 5.9 * py, 7.5 * px, 1.2 * py);
    c.fillRect(0.0 * px, 7.1 * py, 8.5 * px, 1.2 * py);
    c.fillRect(1.0 * px, 8.3 * py, 7.5 * px, 1.2 * py);
    c.fillRect(3.0 * px, 9.5 * py, 5.5 * px, 1.2 * py);
    c.fillRect(5.0 * px, 10.7 * py, 3.5 * px, 1.2 * py);

    // Right Wing (angled up-right)
    c.fillRect(19.0 * px, 3.5 * py, 4.0 * px, 1.2 * py);
    c.fillRect(19.0 * px, 4.7 * py, 6.0 * px, 1.2 * py);
    c.fillRect(19.5 * px, 5.9 * py, 7.5 * px, 1.2 * py);
    c.fillRect(19.5 * px, 7.1 * py, 8.5 * px, 1.2 * py);
    c.fillRect(19.5 * px, 8.3 * py, 7.5 * px, 1.2 * py);
    c.fillRect(19.5 * px, 9.5 * py, 5.5 * px, 1.2 * py);
    c.fillRect(19.5 * px, 10.7 * py, 3.5 * px, 1.2 * py);
  }

  function drawFullHeart(fill) {
    c.fillStyle = fill;
    // Left rounded lobe
    c.fillRect(7.5 * px, 5.0 * py, 5.0 * px, 1.8 * py);
    c.fillRect(7.0 * px, 6.8 * py, 6.0 * px, 1.7 * py);
    // Right rounded lobe
    c.fillRect(15.5 * px, 5.0 * py, 5.0 * px, 1.8 * py);
    c.fillRect(15.0 * px, 6.8 * py, 6.0 * px, 1.7 * py);
    // Main heart body
    c.fillRect(6.5 * px, 8.5 * py, 15.0 * px, 3.5 * py);
    // Standalone lower heart V
    c.fillRect(7.5 * px, 12.0 * py, 13.0 * px, 1.5 * py);
    c.fillRect(8.5 * px, 13.5 * py, 11.0 * px, 1.5 * py);
    c.fillRect(9.5 * px, 15.0 * py, 9.0 * px, 1.5 * py);
    c.fillRect(10.5 * px, 16.5 * py, 7.0 * px, 1.5 * py);
    c.fillRect(11.5 * px, 18.0 * py, 5.0 * px, 1.5 * py);
    c.fillRect(12.5 * px, 19.5 * py, 3.0 * px, 1.5 * py);
    c.fillRect(13.3 * px, 21.0 * py, 1.4 * px, 1.0 * py);
  }

  function drawDualContours(leftCol, rightCol) {
    c.fillStyle = leftCol;
    c.fillRect(6.5 * px, 11.0 * py, 1.0 * px, 1.5 * py);
    c.fillRect(7.5 * px, 12.5 * py, 1.0 * px, 1.5 * py);
    c.fillRect(8.5 * px, 14.0 * py, 1.0 * px, 1.5 * py);
    c.fillRect(9.5 * px, 15.5 * py, 1.0 * px, 1.5 * py);
    c.fillRect(10.5 * px, 17.0 * py, 1.0 * px, 1.5 * py);
    c.fillRect(11.5 * px, 18.5 * py, 1.0 * px, 1.5 * py);
    c.fillRect(12.5 * px, 20.0 * py, 1.0 * px, 1.2 * py);
    c.fillRect(13.3 * px, 21.0 * py, 0.7 * px, 1.0 * py);

    c.fillStyle = rightCol;
    c.fillRect(20.5 * px, 11.0 * py, 1.0 * px, 1.5 * py);
    c.fillRect(19.5 * px, 12.5 * py, 1.0 * px, 1.5 * py);
    c.fillRect(18.5 * px, 14.0 * py, 1.0 * px, 1.5 * py);
    c.fillRect(17.5 * px, 15.5 * py, 1.0 * px, 1.5 * py);
    c.fillRect(16.5 * px, 17.0 * py, 1.0 * px, 1.5 * py);
    c.fillRect(15.5 * px, 18.5 * py, 1.0 * px, 1.5 * py);
    c.fillRect(14.5 * px, 20.0 * py, 1.0 * px, 1.2 * py);
    c.fillRect(14.0 * px, 21.0 * py, 0.7 * px, 1.0 * py);
  }

  function drawEyes(eyeColor) {
    c.fillStyle = eyeColor;
    c.fillRect(9.0 * px, 8.5 * py, 1.8 * px, 3.0 * py);
    c.fillRect(16.5 * px, 9.5 * py, 1.6 * px, 1.6 * py);
  }

  if (id === "original") {
    drawVisor("#00F0FF");
    drawAngledWings("#F6F8FE");
    // Cyan left tip, Pink right tip
    c.fillStyle = "#00F0FF";
    c.fillRect(0.0 * px, 7.1 * py, 1.5 * px, 1.2 * py);
    c.fillRect(1.0 * px, 5.9 * py, 1.2 * px, 1.2 * py);
    c.fillRect(1.0 * px, 8.3 * py, 1.2 * px, 1.2 * py);
    c.fillStyle = "#FF2A93";
    c.fillRect(26.5 * px, 7.1 * py, 1.5 * px, 1.2 * py);
    c.fillRect(25.8 * px, 5.9 * py, 1.2 * px, 1.2 * py);
    c.fillRect(25.8 * px, 8.3 * py, 1.2 * px, 1.2 * py);
    drawFullHeart("#080B10");
    drawDualContours("#00F0FF", "#FF2A93");
    drawEyes("#FFFFFF");
  } else if (id === "pink") {
    drawVisor("#FF2A93");
    drawAngledWings("#FFF0F6");
    c.fillStyle = "#FF2A93";
    c.fillRect(0.0 * px, 7.1 * py, 1.5 * px, 1.2 * py);
    c.fillRect(26.5 * px, 7.1 * py, 1.5 * px, 1.2 * py);
    drawFullHeart("#140816");
    drawDualContours("#FF2A93", "#FF2A93");
    c.fillStyle = "#FF2A93";
    c.fillRect(11.0 * px, 13.5 * py, 6.0 * px, 1.0 * py);
    c.fillRect(12.0 * px, 15.5 * py, 4.0 * px, 1.0 * py);
    drawEyes("#FFE5F0");
  } else if (id === "cyan") {
    drawVisor("#00E5FF");
    drawAngledWings("#E0F7FA");
    c.fillStyle = "#00E5FF";
    c.fillRect(0.0 * px, 7.1 * py, 2.0 * px, 1.2 * py);
    c.fillRect(26.0 * px, 7.1 * py, 2.0 * px, 1.2 * py);
    drawFullHeart("#06121B");
    drawDualContours("#00E5FF", "#00E5FF");
    drawEyes("#FFFFFF");
  } else if (id === "dark") {
    drawVisor("#9D4EDD");
    drawAngledWings("#1B142A");
    c.fillStyle = "#C77DFF";
    c.fillRect(0.0 * px, 7.1 * py, 2.0 * px, 1.2 * py);
    c.fillRect(26.0 * px, 7.1 * py, 2.0 * px, 1.2 * py);
    drawFullHeart("#10091C");
    drawDualContours("#C77DFF", "#9D4EDD");
    drawEyes("#E0AAFF");
  } else if (id === "angel") {
    // Halo
    c.fillStyle = "#FFFFFF";
    c.fillRect(10.5 * px, 1.2 * py, 7.0 * px, 0.8 * py);
    c.fillRect(9.0 * px, 2.0 * py, 1.5 * px, 0.8 * py);
    c.fillRect(17.5 * px, 2.0 * py, 1.5 * px, 0.8 * py);
    drawAngledWings("#FAFAFE");
    c.fillStyle = "#B8C0EC";
    c.fillRect(0.0 * px, 7.1 * py, 1.5 * px, 1.2 * py);
    c.fillRect(26.5 * px, 7.1 * py, 1.5 * px, 1.2 * py);
    drawFullHeart("#C3BEF0");
    drawDualContours("#FFFFFF", "#FFFFFF");
    drawEyes("#FFFFFF");
  } else if (id === "devil") {
    // Horns
    c.fillStyle = "#FF0055";
    c.fillRect(7.5 * px, 2.8 * py, 2.0 * px, 2.2 * py);
    c.fillRect(6.5 * px, 2.0 * py, 1.5 * px, 1.6 * py);
    c.fillRect(18.5 * px, 2.8 * py, 2.0 * px, 2.2 * py);
    c.fillRect(20.0 * px, 2.0 * py, 1.5 * px, 1.6 * py);
    // Bat Wings
    c.fillStyle = "#FF3377";
    c.fillRect(1.0 * px, 5.0 * py, 7.5 * px, 2.5 * py);
    c.fillRect(0.0 * px, 7.5 * py, 8.5 * px, 2.0 * py);
    c.fillRect(2.0 * px, 9.5 * py, 6.5 * px, 2.0 * py);
    c.fillRect(19.5 * px, 5.0 * py, 7.5 * px, 2.5 * py);
    c.fillRect(19.5 * px, 7.5 * py, 8.5 * px, 2.0 * py);
    c.fillRect(19.5 * px, 9.5 * py, 6.5 * px, 2.0 * py);
    drawFullHeart("#180712");
    drawDualContours("#FF0055", "#FF0055");
    drawEyes("#FFB3C6");
  } else if (id === "rainbow") {
    const rainbow = ["#FF3377", "#9D4EDD", "#00B4D8", "#06D6A0", "#FFD166"];
    for (let i = 0; i < 5; i++) {
      c.fillStyle = rainbow[i];
      c.fillRect((5 - i) * px, (3.5 + i * 1.5) * py, (4 + i) * px, 1.5 * py);
      c.fillRect(19 * px, (3.5 + i * 1.5) * py, (4 + i) * px, 1.5 * py);
    }
    drawFullHeart("#0D1018");
    drawDualContours("#FFD166", "#FFD166");
    drawEyes("#FFFFFF");
  } else if (id === "outline") {
    c.strokeStyle = "#00F0FF";
    c.lineWidth = 1.5;
    drawVisor("#00F0FF");
    c.strokeRect(0 * px, 6 * py, 8 * px, 5 * py);
    c.strokeRect(20 * px, 6 * py, 8 * px, 5 * py);
    drawDualContours("#00F0FF", "#00F0FF");
    c.fillStyle = "#00F0FF";
    c.fillRect(13.2 * px, 11.0 * py, 1.6 * px, 1.6 * py);
    drawEyes("#00F0FF");
  } else if (id === "glitch") {
    c.save();
    c.translate(-1.5 * px, 0);
    c.fillStyle = "rgba(255, 0, 85, 0.7)";
    drawAngledWings("rgba(255, 0, 85, 0.7)");
    c.fillRect(6.5 * px, 5.0 * py, 7.5 * px, 16.0 * py);
    c.restore();

    c.save();
    c.translate(1.5 * px, 0);
    c.fillStyle = "rgba(0, 240, 255, 0.7)";
    drawAngledWings("rgba(0, 240, 255, 0.7)");
    c.fillRect(14.0 * px, 5.0 * py, 7.5 * px, 16.0 * py);
    c.restore();

    drawAngledWings("#FFFFFF");
    drawFullHeart("#0C0E18");
    c.fillStyle = "#FF0055";
    c.fillRect(1.0 * px, 7.5 * py, 26.0 * px, 1.0 * py);
    c.fillStyle = "#00F0FF";
    c.fillRect(2.0 * px, 13.5 * py, 24.0 * px, 1.0 * py);
    drawEyes("#FFFFFF");
  } else if (id === "premium") {
    // Crown
    c.fillStyle = "#FFD700";
    c.fillRect(10.5 * px, 1.8 * py, 2.0 * px, 3.0 * py);
    c.fillRect(13.0 * px, 0.8 * py, 2.0 * px, 4.0 * py);
    c.fillRect(15.5 * px, 1.8 * py, 2.0 * px, 3.0 * py);
    c.fillRect(10.5 * px, 4.8 * py, 7.0 * px, 1.0 * py);
    c.fillStyle = "#FF2A93";
    c.fillRect(13.4 * px, 2.5 * py, 1.2 * px, 1.2 * py);
    drawAngledWings("#FFE066");
    c.fillStyle = "#CC8800";
    c.fillRect(3.0 * px, 7.1 * py, 4.0 * px, 1.0 * py);
    c.fillRect(21.0 * px, 7.1 * py, 4.0 * px, 1.0 * py);
    drawFullHeart("#161106");
    drawDualContours("#FFD700", "#FFD700");
    c.fillStyle = "#FFD700";
    c.fillRect(9.0 * px, 11.5 * py, 10.0 * px, 1.0 * py);
    c.fillRect(10.5 * px, 13.8 * py, 7.0 * px, 1.0 * py);
    drawEyes("#FFF5B8");
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
