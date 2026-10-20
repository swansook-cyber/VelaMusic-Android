const { test } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const { JSDOM } = require("jsdom");

const script = fs.readFileSync(path.join(__dirname, "../app/src/main/assets/tv-controls.js"), "utf8");
const card = (id) => `<article class="card" data-row="${id}"><h2 class="title">Song ${id}</h2>
  <div class="player-slot"><audio data-song-id="${id}"></audio></div>
  <button class="js-previous">Previous</button><button class="js-next">Next</button>
  <button class="js-details">Details</button><button class="js-lyrics">Lyrics</button></article>`;

function fixture() {
  const dom = new JSDOM(`<div class="grid">${card(1)}${card(2)}</div><div class="modal hidden"><button class="close">Close</button></div>`, {
    url: "https://music.velalab.net/", runScripts: "outside-only"
  });
  const { window } = dom;
  window.HTMLElement.prototype.getClientRects = function () { return [this.getBoundingClientRect()]; };
  window.HTMLElement.prototype.getBoundingClientRect = function () {
    const row = Number(this.closest(".card")?.dataset.row || 1);
    return { left: 0, right: 300, top: row * 300, bottom: row * 300 + 200 };
  };
  window.HTMLElement.prototype.scrollIntoView = function () {};
  const calls = [];
  window.document.querySelectorAll("audio").forEach((audio) => {
    let paused = true;
    Object.defineProperty(audio, "paused", { get: () => paused });
    audio.play = () => { calls.push(`play:${audio.dataset.songId}`); paused = false; audio.dispatchEvent(new window.Event("play")); return Promise.resolve(); };
    audio.pause = () => { calls.push(`pause:${audio.dataset.songId}`); paused = true; audio.dispatchEvent(new window.Event("pause")); };
  });
  window.eval(script);
  const cards = [...window.document.querySelectorAll(".card")];
  const key = (value) => window.document.activeElement.dispatchEvent(new window.KeyboardEvent("keydown", { key: value, bubbles: true, cancelable: true }));
  return { window, cards, calls, key };
}

test("card, Play/Pause, and other actions keep separate focus and OK targets", () => {
  const { cards, calls, key } = fixture();
  const [first, second] = cards;
  const play = first.querySelector(".tv-play-toggle");
  assert.equal(first.tabIndex, 0);
  first.focus();
  key("ArrowRight");
  assert.equal(first.ownerDocument.activeElement, play);
  key("ArrowRight");
  assert.equal(first.ownerDocument.activeElement, first.querySelector(".js-previous"));
  key("ArrowDown");
  assert.equal(first.ownerDocument.activeElement, second.querySelector(".js-previous"));
  key("ArrowUp");
  assert.equal(first.ownerDocument.activeElement, first.querySelector(".js-previous"));
  first.focus();
  key("Enter");
  assert.deepEqual(calls, ["play:1"]);
  assert.equal(play.getAttribute("aria-pressed"), "true");
  play.click();
  assert.deepEqual(calls, ["play:1", "pause:1"]);
});

test("child click stays on the selected button and touch click still works", () => {
  const { window, cards, calls } = fixture();
  let parentClicks = 0;
  let detailClicks = 0;
  window.document.querySelector(".grid").addEventListener("click", () => parentClicks++);
  cards[0].querySelector(".js-details").addEventListener("click", () => detailClicks++);
  cards[0].querySelector(".js-details").click();
  cards[0].querySelector(".tv-play-toggle").click();
  assert.equal(detailClicks, 1);
  assert.equal(parentClicks, 0);
  assert.deepEqual(calls, ["play:1"]);
});

test("Back closes an open modal once, then falls through to Android history", () => {
  const { window } = fixture();
  const modal = window.document.querySelector(".modal");
  let closes = 0;
  modal.querySelector(".close").addEventListener("click", () => { closes++; modal.classList.add("hidden"); });
  modal.classList.remove("hidden");
  assert.equal(window.__velaTvBack(), true);
  assert.equal(closes, 1);
  assert.equal(window.__velaTvBack(), false);
});

test("launcher densities, TV banner, and manifest resources are consistent", () => {
  const res = path.join(__dirname, "../app/src/main/res");
  for (const [density, size] of Object.entries({ mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 })) {
    const png = fs.readFileSync(path.join(res, `mipmap-${density}/ic_launcher.png`));
    assert.equal(png.readUInt32BE(16), size);
    assert.equal(png.readUInt32BE(20), size);
  }
  const banner = fs.readFileSync(path.join(res, "drawable-nodpi/tv_banner_vela.png"));
  assert.equal(banner.readUInt32BE(16), 320);
  assert.equal(banner.readUInt32BE(20), 180);
  const manifest = fs.readFileSync(path.join(__dirname, "../app/src/main/AndroidManifest.xml"), "utf8");
  assert.match(manifest, /android:icon="@mipmap\/ic_launcher"/);
  assert.match(manifest, /android:roundIcon="@mipmap\/ic_launcher_round"/);
  assert.match(manifest, /android:banner="@drawable\/tv_banner_vela"/);
});
