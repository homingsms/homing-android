#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const icons = {
  Home: "Home09Icon",
  Messages: "Message01Icon",
  Inbox: "InboxIcon",
  Connect: "ConnectIcon",
  Settings: "Settings01Icon",
  Back: "ArrowLeft01Icon",
  ChevronRight: "ArrowRight01Icon",
  ChevronDown: "ArrowDown01Icon",
  Close: "Cancel01Icon",
  Check: "Tick02Icon",
  CheckboxChecked: "CheckmarkSquare02Icon",
  CheckboxEmpty: "SquareIcon",
  CheckCircle: "CheckmarkCircle02Icon",
  Add: "Add01Icon",
  Edit: "PencilEdit02Icon",
  Delete: "Delete02Icon",
  Copy: "Copy01Icon",
  Share: "Share08Icon",
  Search: "Search01Icon",
  FilterOff: "FilterRemoveIcon",
  Refresh: "ArrowReloadHorizontalIcon",
  Sync: "ArrowDataTransferVerticalIcon",
  Transfer: "ArrowDataTransferHorizontalIcon",
  Send: "SentIcon",
  Server: "ServerStack01Icon",
  Cloud: "CloudIcon",
  CloudOff: "CloudOffIcon",
  Webhook: "WebhookIcon",
  QrScan: "QrCodeScanIcon",
  Camera: "Camera01Icon",
  SimCard: "Simcard02Icon",
  Battery: "BatteryFullIcon",
  BatteryCharging: "BatteryCharging01Icon",
  Wifi: "Wifi01Icon",
  Cellular: "CellularNetworkIcon",
  Network: "Globe02Icon",
  Attachment: "Attachment01Icon",
  Image: "Image01Icon",
  File: "File01Icon",
  View: "ViewIcon",
  ViewOff: "ViewOffSlashIcon",
  Coins: "Coins01Icon",
  Wallet: "Wallet01Icon",
  Alert: "Alert02Icon",
  Info: "InformationCircleIcon",
  Key: "Key01Icon",
};

const solidIcons = {
  HomeSolid: "Home09Icon",
  MessagesSolid: "Message01Icon",
  InboxSolid: "InboxIcon",
  ConnectSolid: "ConnectIcon",
  SettingsSolid: "Settings01Icon",
};

const mirrored = new Set(["Back", "ChevronRight", "Send"]);

function usage() {
  console.error("Usage: node scripts/generate-icons.mjs <stroke-rounded package dir> <solid-rounded package dir>");
  process.exit(1);
}

const [strokeDir, solidDir] = process.argv.slice(2);
if (!strokeDir || !solidDir) usage();

const out = path.resolve(
  path.dirname(new URL(import.meta.url).pathname),
  "../core/designsystem/src/main/java/com/homingsms/app/core/designsystem/icon/HomingIcons.kt",
);

const num = (value) => Number.parseFloat(value);

function circlePath(cx, cy, r) {
  return `M${cx - r},${cy}A${r},${r} 0 1,0 ${cx + r},${cy}A${r},${r} 0 1,0 ${cx - r},${cy}Z`;
}

function ellipsePath(cx, cy, rx, ry) {
  return `M${cx - rx},${cy}A${rx},${ry} 0 1,0 ${cx + rx},${cy}A${rx},${ry} 0 1,0 ${cx - rx},${cy}Z`;
}

function rectPath(x, y, w, h, rx, ry) {
  if (!rx && !ry) return `M${x},${y}H${x + w}V${y + h}H${x}Z`;
  rx = Math.min(rx || ry, w / 2);
  ry = Math.min(ry || rx, h / 2);
  return [
    `M${x + rx},${y}`,
    `H${x + w - rx}`,
    `A${rx},${ry} 0 0,1 ${x + w},${y + ry}`,
    `V${y + h - ry}`,
    `A${rx},${ry} 0 0,1 ${x + w - rx},${y + h}`,
    `H${x + rx}`,
    `A${rx},${ry} 0 0,1 ${x},${y + h - ry}`,
    `V${y + ry}`,
    `A${rx},${ry} 0 0,1 ${x + rx},${y}`,
    "Z",
  ].join("");
}

function toPath(tag, a) {
  switch (tag) {
    case "path":
      return a.d;
    case "circle":
      return circlePath(num(a.cx ?? 0), num(a.cy ?? 0), num(a.r));
    case "ellipse":
      return ellipsePath(num(a.cx ?? 0), num(a.cy ?? 0), num(a.rx), num(a.ry));
    case "rect":
      return rectPath(num(a.x ?? 0), num(a.y ?? 0), num(a.width), num(a.height), num(a.rx ?? 0), num(a.ry ?? 0));
    case "line":
      return `M${a.x1},${a.y1}L${a.x2},${a.y2}`;
    case "polyline":
      return `M${a.points.trim().split(/\s+/).join("L")}`;
    case "polygon":
      return `M${a.points.trim().split(/\s+/).join("L")}Z`;
    default:
      throw new Error(`Unsupported element <${tag}>`);
  }
}

const kotlinString = (value) => `"${value.replace(/\\/g, "\\\\").replace(/"/g, '\\"').replace(/\$/g, "\\$")}"`;

async function load(dir, name) {
  const file = path.join(dir, "dist/esm", `${name}.js`);
  if (!fs.existsSync(file)) throw new Error(`${name} not found in ${dir}`);
  return (await import(pathToFileURL(file).href)).default;
}

function emitPath(tag, attrs) {
  const d = toPath(tag, attrs);
  const filled = attrs.fill && attrs.fill !== "none";
  const stroked = attrs.stroke && attrs.stroke !== "none";
  const args = [`pathData = addPathNodes(${kotlinString(d)})`];
  if (filled) args.push("fill = Ink");
  if (attrs.fillRule === "evenodd" || attrs.clipRule === "evenodd") args.push("pathFillType = PathFillType.EvenOdd");
  if (attrs.opacity) args.push(`fillAlpha = ${num(attrs.opacity)}f`, `strokeAlpha = ${num(attrs.opacity)}f`);
  if (stroked) {
    args.push("stroke = Ink");
    args.push(`strokeLineWidth = ${num(attrs.strokeWidth ?? 1.5)}f`);
    args.push(`strokeLineCap = ${attrs.strokeLinecap === "round" ? "StrokeCap.Round" : attrs.strokeLinecap === "square" ? "StrokeCap.Square" : "StrokeCap.Butt"}`);
    args.push(`strokeLineJoin = ${attrs.strokeLinejoin === "round" ? "StrokeJoin.Round" : attrs.strokeLinejoin === "bevel" ? "StrokeJoin.Bevel" : "StrokeJoin.Miter"}`);
  }
  if (!filled && !stroked) args.push("fill = Ink");
  return `            addPath(${args.join(", ")})`;
}

async function emitIcon(kotlinName, dir, sourceName) {
  const data = await load(dir, sourceName);
  const body = data.map(([tag, attrs]) => emitPath(tag, attrs)).join("\n");
  return `    val ${kotlinName}: ImageVector by lazy {
        icon(${kotlinString(kotlinName)}${mirrored.has(kotlinName) ? ", autoMirror = true" : ""}) {
${body}
        }
    }`;
}

const blocks = [];
for (const [name, source] of Object.entries(icons)) blocks.push(await emitIcon(name, strokeDir, source));
for (const [name, source] of Object.entries(solidIcons)) blocks.push(await emitIcon(name, solidDir, source));

const source = `@file:Suppress("ktlint:standard:max-line-length", "MaxLineLength", "ktlint:standard:argument-list-wrapping")

package com.homingsms.app.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

object HomingIcons {
    private val Ink = SolidColor(Color.Black)

    private inline fun icon(name: String, autoMirror: Boolean = false, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
            autoMirror = autoMirror
        )
            .apply(block)
            .build()

${blocks.join("\n\n")}
}
`;

fs.mkdirSync(path.dirname(out), { recursive: true });
fs.writeFileSync(out, source);
console.log(`Wrote ${Object.keys(icons).length + Object.keys(solidIcons).length} icons to ${path.relative(process.cwd(), out)}`);
