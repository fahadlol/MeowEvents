/**
 * Remove the broken Windows SWC optional dependency so Next.js uses Babel.
 * Run on postinstall; only removes on Windows to avoid SWC binary load error.
 */
const fs = require("fs");
const path = require("path");

if (process.platform !== "win32") return;

const swcPath = path.join(__dirname, "..", "node_modules", "@next", "swc-win32-x64-msvc");
if (fs.existsSync(swcPath)) {
  try {
    fs.rmSync(swcPath, { recursive: true });
    console.log("Removed @next/swc-win32-x64-msvc so Next.js uses Babel (avoids Win32 binary error).");
  } catch (e) {
    console.warn("Could not remove SWC folder:", e.message);
  }
}
