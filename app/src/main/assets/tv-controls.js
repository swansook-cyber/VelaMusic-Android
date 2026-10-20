(() => {
  "use strict";
  if (window.__velaTvControlsInstalled) return;
  window.__velaTvControlsInstalled = true;

  const style = document.createElement("style");
  style.textContent = `
    :focus-visible, .grid .card:focus, .grid .card :focus,
    .grid .card :focus-visible, .online-result-card :focus,
    .online-result-card :focus-visible, .karaoke-result-card :focus,
    .karaoke-result-card :focus-visible, .online-playlist-item :focus,
    .online-playlist-item :focus-visible {
      outline: 4px solid #ff4fb5 !important;
      outline-offset: 3px !important;
      box-shadow: 0 0 0 7px rgba(255,79,181,.3) !important;
    }
    .grid .card:focus-within { border-color: #ff4fb5; }
    .grid .tv-play-toggle { min-height: 48px; width: 100%; margin: 8px 0; }
  `;
  (document.head || document.documentElement).append(style);

  const controls = (card) => [card, ...card.querySelectorAll(
    ".tv-play-toggle, .js-previous, .js-next, .js-details, .js-lyrics"
  )].filter((element) => !element.disabled && element.getClientRects().length);

  function updatePlayButton(card) {
    const audio = card.querySelector("audio[data-song-id]");
    const button = card.querySelector(".tv-play-toggle");
    if (!audio || !button) return;
    const playing = !audio.paused && !audio.ended;
    button.textContent = playing ? "หยุดชั่วคราว" : "เล่นเพลง";
    button.setAttribute("aria-label", `${playing ? "Pause" : "Play"} ${card.querySelector(".title")?.textContent || "song"}`);
    button.setAttribute("aria-pressed", String(playing));
  }

  function enhanceCard(card) {
    if (card.dataset.tvControls === "ready") return;
    const slot = card.querySelector(".player-slot");
    const audio = slot?.querySelector("audio[data-song-id]");
    if (!slot || !audio) return;
    card.dataset.tvControls = "ready";
    card.tabIndex = 0;
    card.setAttribute("aria-label", card.querySelector(".title")?.textContent || "เพลง");
    const button = document.createElement("button");
    button.type = "button";
    button.className = "button secondary tv-play-toggle";
    slot.before(button);
    button.addEventListener("click", (event) => {
      event.stopPropagation();
      const currentAudio = card.querySelector("audio[data-song-id]");
      if (!currentAudio) return;
      if (currentAudio.paused || currentAudio.ended) {
        const request = currentAudio.play();
        if (request?.catch) request.catch(() => updatePlayButton(card));
      } else currentAudio.pause();
    });
    // The page delegates no card click today; keep child actions isolated if it does later.
    card.addEventListener("click", (event) => {
      if (event.target.closest("button, audio")) event.stopPropagation();
    });
    updatePlayButton(card);
  }

  function enhance() {
    document.querySelectorAll(".grid .card").forEach(enhanceCard);
  }
  enhance();
  new MutationObserver(enhance).observe(document.body, { childList: true, subtree: true });
  for (const name of ["play", "pause", "ended"]) {
    document.addEventListener(name, (event) => {
      const card = event.target.closest?.(".grid .card");
      if (card) updatePlayButton(card);
    }, true);
  }

  document.addEventListener("keydown", (event) => {
    const card = document.activeElement?.closest?.(".grid .card");
    if (!card || event.altKey || event.ctrlKey || event.metaKey) return;
    const active = document.activeElement;
    if (active.matches("input, textarea, select, audio, [contenteditable]")) return;
    if ((event.key === "Enter" || event.key === " ") && active === card) {
      event.preventDefault();
      event.stopPropagation();
      card.querySelector(".tv-play-toggle")?.click();
      return;
    }
    if (!["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown"].includes(event.key)) return;
    const row = controls(card);
    const position = row.indexOf(active);
    if (position < 0) return;
    let next;
    if (event.key === "ArrowLeft") next = row[position - 1];
    if (event.key === "ArrowRight") next = row[position + 1];
    if (event.key === "ArrowUp" || event.key === "ArrowDown") {
      const currentRect = card.getBoundingClientRect();
      const candidates = [...document.querySelectorAll(".grid .card")].filter((other) => {
        if (other === card || !other.getClientRects().length) return false;
        const rect = other.getBoundingClientRect();
        return event.key === "ArrowUp" ? rect.bottom <= currentRect.top + 8 : rect.top >= currentRect.bottom - 8;
      });
      candidates.sort((a, b) => {
        const distance = (other) => {
          const rect = other.getBoundingClientRect();
          const dx = (rect.left + rect.right - currentRect.left - currentRect.right) / 2;
          const dy = (rect.top + rect.bottom - currentRect.top - currentRect.bottom) / 2;
          return Math.abs(dy) + Math.abs(dx) * 2;
        };
        return distance(a) - distance(b);
      });
      if (candidates[0]) next = controls(candidates[0])[position] || candidates[0];
    }
    if (next) {
      event.preventDefault();
      event.stopPropagation();
      next.focus();
      next.scrollIntoView({ block: "nearest", inline: "nearest" });
    }
  });

  window.__velaTvBack = () => {
    const modal = [...document.querySelectorAll(".modal:not(.hidden)")]
      .reverse().find((element) => element.getClientRects().length);
    const close = modal?.querySelector("button.close");
    if (!close) return false;
    close.click();
    return true;
  };
})();
