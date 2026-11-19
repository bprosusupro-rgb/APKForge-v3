(function () {
  "use strict";

  if (window.__apkForgeTouchAdapterV1 || !document.documentElement) return;
  window.__apkForgeTouchAdapterV1 = true;

  var selector = 'a,button,label,summary,select,[role="button"],[role="link"],[role="menuitem"],[role="option"],[role="switch"],[role="checkbox"],[role="radio"],[role="tab"],[onclick],[tabindex]:not([tabindex="-1"]),input[type="button"],input[type="submit"],input[type="reset"],input[type="checkbox"],input[type="radio"]';
  var records = [];
  var scheduled = false;
  var mutationTimer = 0;
  var MAX_MACRO_STEPS = 120;
  var MAX_MACRO_DURATION_MS = 180000;
  var style = document.createElement("style");
  style.textContent = "[data-apkforge-tool-ui],[data-apkforge-tool-ui] *{box-sizing:border-box!important;font-family:system-ui,sans-serif!important}" +
    "[data-apkforge-control-row]>button,[data-apkforge-add-row]>button,[data-apkforge-macro-footer]>button{width:100%!important;max-width:100%!important;min-width:0!important}" +
    "[data-apkforge-step] button{width:auto!important;max-width:100%!important;min-width:0!important}" +
    "[data-apkforge-tools-toggle]{width:56px!important;height:56px!important;min-width:56px!important;min-height:56px!important}" +
    "[data-apkforge-touch-overlay]{position:fixed;display:block;" +
    "margin:0;padding:0;border:0;background:transparent;opacity:0;" +
    "pointer-events:auto;z-index:2147483000;touch-action:manipulation;" +
    "-webkit-user-select:none;user-select:none;-webkit-tap-highlight-color:transparent}";
  (document.head || document.documentElement).appendChild(style);

  if (!document.querySelector('meta[name="viewport"]')) {
    var viewport = document.createElement("meta");
    viewport.name = "viewport";
    viewport.content = "width=device-width, initial-scale=1, viewport-fit=cover";
    (document.head || document.documentElement).appendChild(viewport);
  }

  function makeOverlay(target) {
    var overlay = document.createElement("div");
    overlay.setAttribute("data-apkforge-touch-overlay", "true");
    overlay.setAttribute("aria-hidden", "true");
    overlay.setAttribute("role", "presentation");
    overlay.tabIndex = -1;
    overlay.__apkForgeTouchTarget = target;
    overlay.addEventListener("click", function (event) {
      event.preventDefault();
      event.stopPropagation();
      if (target.isConnected && !target.disabled && target.getAttribute("aria-disabled") !== "true") {
        target.click();
      }
    });
    document.documentElement.appendChild(overlay);
    return overlay;
  }

  function setRect(overlay, left, top, width, height) {
    if (width <= 0 || height <= 0) {
      overlay.style.display = "none";
      return;
    }
    var values = {
      display: "block",
      left: left.toFixed(2) + "px",
      top: top.toFixed(2) + "px",
      width: width.toFixed(2) + "px",
      height: height.toFixed(2) + "px"
    };
    Object.keys(values).forEach(function (key) {
      if (overlay.style[key] !== values[key]) overlay.style[key] = values[key];
    });
  }

  function removeRecord(record) {
    record.overlays.forEach(function (overlay) { overlay.remove(); });
  }

  function refreshTargets() {
    scheduled = false;
    var nodes = document.querySelectorAll(selector);
    var candidates = [];
    var limit = Math.min(nodes.length, 160);

    for (var i = 0; i < limit; i++) {
      var node = nodes[i];
      if (!(node instanceof HTMLElement) || node.disabled ||
          isToolElement(node) ||
          node.getAttribute("aria-disabled") === "true" ||
          (node.parentElement && node.parentElement.closest(selector))) continue;
      var rect = node.getBoundingClientRect();
      var css = window.getComputedStyle(node);
      if (rect.width <= 0 || rect.height <= 0 || css.display === "none" ||
          css.visibility === "hidden" || Number(css.opacity) === 0 ||
          css.pointerEvents === "none") continue;

      var hx = Math.max(0, Math.min(22, (44 - rect.width) / 2));
      var hy = Math.max(0, Math.min(22, (44 - rect.height) / 2));
      if (hx === 0 && hy === 0) continue;
      candidates.push({ node: node, rect: rect, hx: hx, hy: hy, blocked: false });
    }

    for (var a = 0; a < candidates.length; a++) {
      var first = candidates[a];
      var firstBox = { left: first.rect.left - first.hx, right: first.rect.right + first.hx,
        top: first.rect.top - first.hy, bottom: first.rect.bottom + first.hy };
      for (var b = a + 1; b < candidates.length; b++) {
        var second = candidates[b];
        var secondBox = { left: second.rect.left - second.hx, right: second.rect.right + second.hx,
          top: second.rect.top - second.hy, bottom: second.rect.bottom + second.hy };
        if (firstBox.left < secondBox.right && firstBox.right > secondBox.left &&
            firstBox.top < secondBox.bottom && firstBox.bottom > secondBox.top) {
          first.blocked = true;
          second.blocked = true;
        }
      }
    }

    var nextRecords = [];
    candidates.forEach(function (candidate) {
      if (candidate.blocked || !candidate.node.isConnected) return;
      var old = records.find(function (record) { return record.target === candidate.node; });
      if (!old) old = { target: candidate.node, overlays: [] };
      while (old.overlays.length < 4) old.overlays.push(makeOverlay(old.target));
      var r = candidate.rect, x = candidate.hx, y = candidate.hy;
      setRect(old.overlays[0], r.left - x, r.top - y, x, r.height + 2 * y);
      setRect(old.overlays[1], r.right, r.top - y, x, r.height + 2 * y);
      setRect(old.overlays[2], r.left, r.top - y, r.width, y);
      setRect(old.overlays[3], r.left, r.bottom, r.width, y);
      nextRecords.push(old);
    });

    records.forEach(function (record) {
      if (!nextRecords.some(function (next) { return next.target === record.target; })) removeRecord(record);
    });
    records = nextRecords;
  }

  function scheduleRefresh() {
    if (scheduled) return;
    scheduled = true;
    window.requestAnimationFrame(refreshTargets);
  }

  function scheduleMutationRefresh() {
    if (mutationTimer) window.clearTimeout(mutationTimer);
    mutationTimer = window.setTimeout(function () {
      mutationTimer = 0;
      scheduleRefresh();
    }, 100);
  }

  var macroStage = document.createElement("div");
  macroStage.setAttribute("data-apkforge-macro-stage", "true");
  macroStage.setAttribute("data-apkforge-tool-ui", "true");
  macroStage.setAttribute("role", "group");
  macroStage.setAttribute("aria-label", "Position macro taps on the game");
  macroStage.style.cssText = "position:fixed;inset:0;z-index:2147483645;display:none;pointer-events:none;" +
    "-webkit-tap-highlight-color:transparent;touch-action:none;";
  macroStage.innerHTML = '<div data-apkforge-stage-dots style="position:absolute;inset:0;pointer-events:none"></div>' +
    '<div style="position:absolute;left:50%;top:max(12px,env(safe-area-inset-top));transform:translateX(-50%);' +
    'display:flex;align-items:center;gap:10px;max-width:calc(100vw - 24px);padding:8px 10px 8px 14px;' +
    'border:1px solid rgba(255,255,255,.2);border-radius:16px;background:rgba(17,27,32,.96);' +
    'box-shadow:0 8px 28px rgba(0,0,0,.35);color:#f4fffc;font:500 13px/1.3 system-ui,sans-serif;pointer-events:auto">' +
    '<span data-apkforge-stage-hint>Tap the game to position the selected marker; drag dots to move them.</span>' +
    '<button type="button" data-apkforge-stage-done style="flex:none;min-width:48px;min-height:44px;padding:0 12px;' +
    'border:0;border-radius:11px;background:#10b99f;color:#06211e;font:600 13px system-ui,sans-serif">Done</button></div>';
  (document.body || document.documentElement).appendChild(macroStage);

  var tools = document.createElement("div");
  tools.setAttribute("data-apkforge-tool-ui", "true");
  tools.setAttribute("role", "group");
  tools.setAttribute("aria-label", "Game input tools");
  tools.style.cssText = "position:fixed!important;left:auto!important;top:auto!important;right:max(12px,env(safe-area-inset-right))!important;" +
    "bottom:max(12px,env(safe-area-inset-bottom));z-index:2147483647;display:flex;" +
    "width:auto!important;max-width:calc(100vw - 16px)!important;flex-direction:column;align-items:flex-end;gap:10px;font:500 14px/1.2 system-ui,sans-serif;" +
    "-webkit-tap-highlight-color:transparent;touch-action:manipulation;";
  tools.innerHTML = '<div data-apkforge-tool-menu hidden style="display:none;flex-direction:column;align-items:stretch;gap:8px;' +
    'width:min(384px,calc(100vw - 24px));max-width:calc(100vw - 24px);max-height:min(76vh,calc(100vh - 112px));' +
    'overflow-y:auto;overscroll-behavior:contain;padding:12px;border:1px solid rgba(145,210,197,.2);' +
    'border-radius:20px;background:#101d21;box-shadow:0 12px 36px rgba(0,0,0,.48)\">' +
    '<div style="display:flex;flex-direction:column;gap:3px;padding:2px 4px 8px;border-bottom:1px solid rgba(145,210,197,.18)">' +
    '<div style="color:#f4fffc;font:700 16px/1.25 system-ui,sans-serif">Game tools</div>' +
    '<div style="color:#9fb5b1;font:12px/1.35 system-ui,sans-serif">Pointer, keyboard &amp; editable macros</div></div>' +
    '<div data-apkforge-control-row style="display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:8px">' +
    '<button type="button" data-apkforge-mouse-toggle aria-label="Turn on mouse pointer" aria-pressed="false" ' +
    'style="min-height:48px;padding:0 10px;border:0;border-radius:12px;background:#24383b;color:#f4fffc;font:600 13px system-ui,sans-serif">Mouse pointer</button>' +
    '<button type="button" data-apkforge-keyboard aria-label="Open keyboard" ' +
    'style="min-height:48px;padding:0 10px;border:0;border-radius:12px;background:#10b99f;color:#06211e;font:600 13px system-ui,sans-serif">Keyboard</button></div>' +
    '<div style="display:flex;flex-direction:column;gap:6px;padding-top:2px">' +
    '<div style="color:#f4fffc;font:650 13px system-ui,sans-serif">Build your macro</div>' +
    '<div data-apkforge-add-row style="display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:6px">' +
    '<button type="button" data-apkforge-macro-add-tap style="min-height:44px;padding:0 4px;border:0;border-radius:11px;background:#10b99f;color:#06211e;font:600 11px system-ui,sans-serif">+ Add tap</button>' +
    '<button type="button" data-apkforge-macro-add-scroll style="min-height:44px;padding:0 3px;border:0;border-radius:11px;background:#294b4a;color:#f4fffc;font:600 11px system-ui,sans-serif">+ Add scroll</button>' +
    '<button type="button" data-apkforge-macro-add-wait style="min-height:44px;padding:0 3px;border:0;border-radius:11px;background:#294b4a;color:#f4fffc;font:600 11px system-ui,sans-serif">+ Add wait</button></div></div>' +
    '<div style="padding:9px 11px;border:1px solid rgba(145,210,197,.24);border-radius:12px;background:#152729;' +
    'color:#b9ceca;font:12px/1.4 system-ui,sans-serif">Tap a step to place it on the game. Drag numbered markers to adjust them.</div>' +
    '<div data-apkforge-macro-status role="status" aria-live="polite" style="padding:3px 4px;color:#b8ccc8;font:12px/1.35 system-ui,sans-serif">No actions yet · add a step</div>' +
    '<div data-apkforge-macro-steps style="display:flex;flex-direction:column;gap:7px;max-height:28vh;overflow-y:auto;overscroll-behavior:contain"></div>' +
    '<div data-apkforge-macro-footer style="position:sticky;bottom:0;display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px;padding-top:8px;background:linear-gradient(to bottom,rgba(16,29,33,.86),#101d21 28%)">' +
    '<button type="button" data-apkforge-macro-play disabled style="min-height:48px;padding:0 12px;border:0;border-radius:12px;background:#34534e;color:#f4fffc;font:600 14px system-ui,sans-serif">Play macro</button>' +
    '<button type="button" data-apkforge-macro-clear aria-label="Clear all steps" style="min-height:48px;padding:0 12px;border:1px solid rgba(255,255,255,.2);border-radius:12px;background:transparent;color:#d5e3e0;font:500 12px system-ui,sans-serif">Clear</button></div>' +
    '</div><button type="button" data-apkforge-tools-toggle aria-label="Open game input tools" aria-expanded="false" ' +
    'style="width:56px;height:56px;display:grid;place-items:center;border:1px solid rgba(255,255,255,.25);' +
    'border-radius:50%;background:#10b99f;color:#06211e;box-shadow:0 4px 18px rgba(0,0,0,.35)">' +
    '<svg aria-hidden="true" viewBox="0 0 24 24" width="25" height="25" fill="none" stroke="currentColor" stroke-width="1.8">' +
    '<path d="M7 3.8a5 5 0 0 1 10 0v8.4a5 5 0 0 1-10 0z"/><path d="M12 4v5"/></svg></button>' +
    '<div data-apkforge-cursor aria-hidden="true" style="display:none;position:fixed;left:50vw;top:50vh;' +
    'width:26px;height:26px;transform:translate(-3px,-3px);pointer-events:none;z-index:2147483646;' +
    'border:2px solid #fff;border-radius:50%;background:rgba(16,185,159,.45);box-shadow:0 0 0 2px #092621,0 2px 10px #000"></div>';
  (document.body || document.documentElement).appendChild(tools);

  var toolMenu = tools.querySelector("[data-apkforge-tool-menu]");
  var toolsToggle = tools.querySelector("[data-apkforge-tools-toggle]");
  var mouseToggle = tools.querySelector("[data-apkforge-mouse-toggle]");
  var keyboardButton = tools.querySelector("[data-apkforge-keyboard]");
  var macroStatus = tools.querySelector("[data-apkforge-macro-status]");
  var macroAddTapButton = tools.querySelector("[data-apkforge-macro-add-tap]");
  var macroAddScrollButton = tools.querySelector("[data-apkforge-macro-add-scroll]");
  var macroAddWaitButton = tools.querySelector("[data-apkforge-macro-add-wait]");
  var macroStageDots = macroStage.querySelector("[data-apkforge-stage-dots]");
  var macroStageDone = macroStage.querySelector("[data-apkforge-stage-done]");
  var macroStepsView = tools.querySelector("[data-apkforge-macro-steps]");
  var macroPlayButton = tools.querySelector("[data-apkforge-macro-play]");
  var macroClearButton = tools.querySelector("[data-apkforge-macro-clear]");
  var cursor = tools.querySelector("[data-apkforge-cursor]");
  var mouseMode = false;
  var lastTextTarget = null;
  var mouseContact = null;
  var macroKey = "__apkforge_macro_v1";
  var macroSteps = [];
  var macroPlaying = false;
  var macroPlayToken = 0;
  var macroTimers = [];
  var macroDurationTimer = 0;
  var macroStagePointerId = null;
  var keyboardSink = document.createElement("textarea");
  keyboardSink.setAttribute("aria-label", "Game keyboard input");
  keyboardSink.setAttribute("autocomplete", "off");
  keyboardSink.setAttribute("autocapitalize", "off");
  keyboardSink.setAttribute("data-apkforge-tool-ui", "true");
  keyboardSink.style.cssText = "position:fixed;left:-1000px;top:0;width:1px;height:1px;opacity:.01;" +
    "font-size:16px;pointer-events:none;";
  (document.body || document.documentElement).appendChild(keyboardSink);

  function isToolElement(node) {
    return !!(node && node.closest && node.closest('[data-apkforge-tool-ui]'));
  }

  function openMenu(open) {
    toolMenu.hidden = !open;
    toolMenu.style.display = open ? "flex" : "none";
    toolsToggle.setAttribute("aria-expanded", open ? "true" : "false");
    toolsToggle.setAttribute("aria-label", open ? "Close game input tools" : "Open game input tools");
    try { window.APKForgeLocalSaves.onToolsExpanded(open); } catch (ignored) {}
  }

  function setMouseMode(enabled) {
    mouseMode = enabled;
    cursor.style.display = enabled ? "block" : "none";
    mouseToggle.setAttribute("aria-pressed", enabled ? "true" : "false");
    mouseToggle.setAttribute("aria-label", enabled ? "Turn off mouse pointer" : "Turn on mouse pointer");
    mouseToggle.style.background = enabled ? "#10b99f" : "#24383b";
    mouseToggle.style.color = enabled ? "#06211e" : "#f4fffc";
  }

  function loadMacro() {
    try {
      var saved = JSON.parse(localStorage.getItem(macroKey) || "null");
      if (saved && (saved.version === 1 || saved.version === 2) && Array.isArray(saved.steps) && saved.steps.length <= MAX_MACRO_STEPS) {
        macroSteps = saved.steps.filter(function (step) {
          return step && (step.type === "tap" || step.type === "scroll" || step.type === "wait") &&
            Number.isFinite(step.delay) && step.delay >= 0 &&
            (step.type === "tap" ? Number.isFinite(step.x) && Number.isFinite(step.y) : step.type === "scroll" ?
              Number.isFinite(step.dx) && Number.isFinite(step.dy) : Number.isFinite(step.duration));
        });
        macroSteps.forEach(function (step) { step.repeatCount = Math.max(1, Math.min(50, Number(step.repeatCount) || 1));
          step.repeatInterval = Math.max(0, Math.min(15000, Number(step.repeatInterval) || 0)); });
        persistMacro();
      }
    } catch (ignored) { macroSteps = []; }
  }

  function updateMacroUi() {
    macroPlayButton.disabled = macroPlaying || macroSteps.length === 0;
    macroPlayButton.style.opacity = macroPlayButton.disabled ? ".55" : "1";
    macroPlayButton.textContent = macroPlaying ? "Stop playback" : "Play macro";
    macroPlayButton.style.background = macroPlaying ? "#d48a32" : "#34534e";
    macroPlayButton.style.color = macroPlaying ? "#151b1b" : "#f4fffc";
    macroClearButton.disabled = macroPlaying || macroSteps.length === 0;
    macroClearButton.style.opacity = macroClearButton.disabled ? ".55" : "1";
    macroStatus.textContent = macroPlaying ? "Playing macro…" : macroSteps.length ? macroSteps.length + " editable steps · saved on this device" :
        "No actions yet · add steps below";
    renderMacroStage();
    renderMacroSteps();
  }

  function renderMacroStage() {
    if (!macroStage || !macroStageDots) return;
    macroStage.style.display = macroPickingStep >= 0 ? "block" : "none";
    macroStageDots.textContent = "";
    macroSteps.forEach(function (step, index) {
      if (step.type !== "tap") return;
      var dot = document.createElement("button");
      dot.type = "button";
      dot.setAttribute("data-apkforge-stage-dot", String(index));
      dot.setAttribute("aria-label", "Tap " + (index + 1) + " position on game, drag to move");
      dot.textContent = String(index + 1);
      dot.style.cssText = "position:absolute;left:" + (Math.max(0, Math.min(1, step.x)) * 100) +
        "%;top:" + (Math.max(0, Math.min(1, step.y)) * 100) +
        "%;width:44px;height:44px;transform:translate(-50%,-50%);display:grid;place-items:center;" +
        "padding:0;border:2px solid #d8fff7;border-radius:50%;background:#10b99f;color:#06211e;" +
        "box-shadow:0 0 0 3px rgba(7,28,28,.72),0 3px 12px rgba(0,0,0,.7);font:700 14px system-ui,sans-serif;" +
        "touch-action:none;pointer-events:auto;z-index:" + (100 + index) + ";" +
        (index === macroPickingStep ? "outline:3px solid #f7c35c;outline-offset:3px;" : "");
      dot.addEventListener("pointerdown", function (event) {
        if (macroPlaying) return;
        macroStagePointerId = event.pointerId;
        try { dot.setPointerCapture(event.pointerId); } catch (ignored) {}
        event.preventDefault(); event.stopPropagation();
      });
      dot.addEventListener("pointermove", function (event) {
        if (macroStagePointerId !== event.pointerId) return;
        var rect = macroStage.getBoundingClientRect();
        step.x = Math.max(0, Math.min(1, (event.clientX - rect.left) / Math.max(1, rect.width)));
        step.y = Math.max(0, Math.min(1, (event.clientY - rect.top) / Math.max(1, rect.height)));
        dot.style.left = (step.x * 100) + "%";
        dot.style.top = (step.y * 100) + "%";
        event.preventDefault();
      });
      dot.addEventListener("pointerup", function (event) {
        if (macroStagePointerId !== event.pointerId) return;
        macroStagePointerId = null;
        persistMacro(); updateMacroUi();
        event.preventDefault(); event.stopPropagation();
      });
      dot.addEventListener("pointercancel", function () {
        macroStagePointerId = null; persistMacro(); updateMacroUi();
      });
      macroStageDots.appendChild(dot);
    });
  }

  function persistMacro() {
    try { localStorage.setItem(macroKey, JSON.stringify({ version: 2, steps: macroSteps })); }
    catch (ignored) { macroStatus.textContent = "Could not save macro in this game’s local storage"; }
  }

  function stopMacroPlayback() {
    macroPlayToken++;
    macroTimers.forEach(function (timer) { window.clearTimeout(timer); });
    macroTimers = [];
    if (macroDurationTimer) window.clearTimeout(macroDurationTimer);
    macroDurationTimer = 0;
    macroPlaying = false;
    updateMacroUi();
  }

  function renderMacroSteps() {
    if (!macroStepsView) return;
    macroStepsView.textContent = "";
    macroSteps.forEach(function (step, index) {
      var card = document.createElement("div");
      card.draggable = !macroPlaying;
      card.setAttribute("data-apkforge-step", String(index));
      card.style.cssText = "padding:9px;border:1px solid rgba(255,255,255,.13);border-radius:13px;background:#203234;color:#eef7f5;font:12px system-ui,sans-serif";
      var heading = document.createElement("div"); heading.textContent = "⠿  " + (index + 1) + ". " + step.type.toUpperCase();
      heading.style.cssText = "font-weight:700;margin-bottom:7px;cursor:grab;touch-action:none"; card.appendChild(heading);
      heading.addEventListener("pointerdown", function (event) {
        if (event.pointerType !== "touch" || macroPlaying) return;
        macroTouchDragIndex = index;
        try { heading.setPointerCapture(event.pointerId); } catch (ignored) {}
        event.preventDefault();
      });
      heading.addEventListener("pointerup", function (event) {
        if (macroTouchDragIndex < 0) return;
        var from = macroTouchDragIndex;
        macroTouchDragIndex = -1;
        var target = document.elementFromPoint(event.clientX, event.clientY);
        var targetCard = target && target.closest ? target.closest("[data-apkforge-step]") : null;
        var to = targetCard ? Number(targetCard.getAttribute("data-apkforge-step")) : -1;
        if (to < 0 || from === to || from >= macroSteps.length || to >= macroSteps.length) return;
        var moved = macroSteps.splice(from, 1)[0]; macroSteps.splice(to, 0, moved);
        persistMacro(); updateMacroUi();
      });
      heading.addEventListener("pointercancel", function () { macroTouchDragIndex = -1; });
      var fields = document.createElement("div"); fields.style.cssText = "display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:6px";
      function field(label, attr, value, min, max, suffix) {
        var wrap = document.createElement("label"); wrap.textContent = label;
        wrap.style.cssText = "display:flex;flex-direction:column;gap:3px;color:#bed0cd;font-size:10px";
        var input = document.createElement("input"); input.type = "number"; input.setAttribute(attr, ""); input.value = String(value);
        input.min = String(min); input.max = String(max); input.step = "1";
        input.style.cssText = "box-sizing:border-box;width:100%;min-height:34px;padding:4px 6px;border:1px solid #49605f;border-radius:8px;background:#132325;color:#fff;font:12px system-ui";
        input.addEventListener("change", function () {
          var parsed = Number(input.value); if (!Number.isFinite(parsed)) parsed = 0;
          var property = attr.replace("data-apkforge-step-", "");
          if (property === "repeat-interval") property = "repeatInterval";
          if (property === "repeat") property = "repeatCount";
          if (property === "x" || property === "y") parsed /= 100;
          step[property] = Math.max(property === "x" || property === "y" ? 0 : min,
            Math.min(property === "x" || property === "y" ? 1 : max, parsed));
          persistMacro(); updateMacroUi();
        });
        wrap.appendChild(input); fields.appendChild(wrap);
      }
      if (step.type === "tap") { field("X · %", "data-apkforge-step-x", Math.round(step.x * 100), 0, 100); field("Y · %", "data-apkforge-step-y", Math.round(step.y * 100), 0, 100); }
      if (step.type === "scroll") { field("Horizontal px", "data-apkforge-step-dx", step.dx, -10000, 10000); field("Vertical px", "data-apkforge-step-dy", step.dy, -10000, 10000); }
      if (step.type === "wait") field("Wait · ms", "data-apkforge-step-duration", step.duration, 0, 15000);
      field("Before step · ms", "data-apkforge-step-delay", step.delay || 0, 0, 15000);
      field("Repeat", "data-apkforge-step-repeat", step.repeatCount || 1, 1, 50);
      field("Repeat gap · ms", "data-apkforge-step-repeat-interval", step.repeatInterval || 0, 0, 15000);
      card.appendChild(fields);
      if (step.type === "tap") {
        var pick = document.createElement("button"); pick.type = "button"; pick.textContent = "Position on game";
        pick.setAttribute("data-apkforge-stage-place", "true");
        pick.style.cssText = "margin-top:7px;min-height:44px;border:0;border-radius:9px;padding:0 12px;background:#315551;color:#fff;font:600 12px system-ui";
        pick.addEventListener("click", function () { macroPickingStep = index; macroPickingMode = "tap";
          macroStatus.textContent = "Tap the game to position the selected marker"; openMenu(false); updateMacroUi(); }); card.appendChild(pick);
      } else if (step.type === "scroll") {
        var pickScroll = document.createElement("button"); pickScroll.type = "button";
        pickScroll.textContent = step.path ? "Change scroll area" : "Pick scroll area (optional)";
        pickScroll.style.cssText = "margin-top:7px;min-height:34px;border:0;border-radius:9px;padding:0 9px;background:#315551;color:#fff;font:600 11px system-ui";
        pickScroll.addEventListener("click", function () { macroPickingStep = index; macroPickingMode = "scroll";
          macroStatus.textContent = "Tap inside the game area that should scroll…"; openMenu(false); }); card.appendChild(pickScroll);
      }
      var actions = document.createElement("div"); actions.style.cssText = "display:flex;gap:5px;margin-top:7px";
      [["↑", -1, "Move step up"], ["↓", 1, "Move step down"]].forEach(function (item) {
        var move = document.createElement("button"); move.type = "button"; move.textContent = item[0]; move.setAttribute("aria-label", item[2]);
        move.disabled = macroPlaying || index + item[1] < 0 || index + item[1] >= macroSteps.length;
        move.style.cssText = "min-width:35px;min-height:32px;border:1px solid #49605f;border-radius:8px;background:#152629;color:#fff";
        move.addEventListener("click", function () { var other = index + item[1]; if (other < 0 || other >= macroSteps.length) return;
          var temp = macroSteps[index]; macroSteps[index] = macroSteps[other]; macroSteps[other] = temp; persistMacro(); updateMacroUi(); }); actions.appendChild(move);
      });
      var remove = document.createElement("button"); remove.type = "button"; remove.textContent = "Remove"; remove.disabled = macroPlaying;
      remove.style.cssText = "margin-left:auto;min-height:32px;border:1px solid #7b5551;border-radius:8px;background:#402e2e;color:#fff;padding:0 9px";
      remove.addEventListener("click", function () { macroSteps.splice(index, 1); persistMacro(); updateMacroUi(); }); actions.appendChild(remove); card.appendChild(actions);
      card.addEventListener("dragstart", function (event) { macroDragIndex = index; event.dataTransfer.effectAllowed = "move"; });
      card.addEventListener("dragover", function (event) { event.preventDefault(); event.dataTransfer.dropEffect = "move"; });
      card.addEventListener("drop", function (event) { event.preventDefault(); if (macroDragIndex < 0 || macroDragIndex === index) return;
        var moved = macroSteps.splice(macroDragIndex, 1)[0]; macroSteps.splice(index, 0, moved); macroDragIndex = -1; persistMacro(); updateMacroUi(); });
      macroStepsView.appendChild(card);
    });
  }
  var macroPickingStep = -1, macroDragIndex = -1, macroTouchDragIndex = -1, macroPickingMode = "tap";
  function addManualStep(type) {
    if (macroPlaying || macroSteps.length >= MAX_MACRO_STEPS) return;
    var step = { type: type, delay: 0, repeatCount: 1, repeatInterval: 0 };
    if (type === "tap") { step.x = .5; step.y = .5; }
    if (type === "scroll") { step.dx = 0; step.dy = 400; }
    if (type === "wait") step.duration = 1000;
    macroSteps.push(step); persistMacro(); updateMacroUi();
  }

  function elementPath(element) {
    if (!element || element === document || element === document.body || element === document.documentElement ||
        element === document.scrollingElement) return "";
    var path = [];
    var node = element;
    while (node && node.nodeType === 1 && node !== document.body && path.length < 12) {
      var tag = node.tagName.toLowerCase();
      var sameTag = node.parentElement ? Array.prototype.filter.call(node.parentElement.children, function (sibling) {
        return sibling.tagName === node.tagName;
      }) : [];
      path.unshift(tag + (sameTag.length > 1 ? ":nth-of-type(" + (sameTag.indexOf(node) + 1) + ")" : ""));
      node = node.parentElement;
    }
    return path.join(" > ");
  }

  function dispatchTap(xRatio, yRatio) {
    var x = Math.max(0, Math.min(window.innerWidth - 1, xRatio * window.innerWidth));
    var y = Math.max(0, Math.min(window.innerHeight - 1, yRatio * window.innerHeight));
    var hit = document.elementFromPoint(x, y);
    var target = hit && hit.__apkForgeTouchTarget ? hit.__apkForgeTouchTarget : hit;
    if (!target || isToolElement(target)) return;
    dispatchMouse("mousedown", x, y, target, true);
    dispatchMouse("mouseup", x, y, target, true);
    dispatchMouse("click", x, y, target, true);
  }

  function dispatchScroll(step) {
    var target = null;
    if (step.path) {
      try { target = document.querySelector(step.path); } catch (ignored) {}
    }
    if (target && target !== document.body && target !== document.documentElement && target !== document.scrollingElement) {
      target.scrollBy(step.dx, step.dy);
    } else {
      window.scrollBy(step.dx, step.dy);
    }
  }

  function playMacro() {
    if (macroPlaying || !macroSteps.length) return;
    macroPlaying = true;
    macroDurationTimer = window.setTimeout(stopMacroPlayback, MAX_MACRO_DURATION_MS);
    var token = ++macroPlayToken;
    updateMacroUi();
    var index = 0;
    function next() {
      if (!macroPlaying || token !== macroPlayToken || index >= macroSteps.length) {
        if (token === macroPlayToken) stopMacroPlayback();
        return;
      }
      var step = macroSteps[index++];
      var timer = window.setTimeout(function () {
        if (!macroPlaying || token !== macroPlayToken) return;
        var repeats = Math.max(1, Math.min(50, Number(step.repeatCount) || 1));
        for (var r = 0; r < repeats; r++) {
          var actionTimer = window.setTimeout(function (copy) { return function () {
            if (!macroPlaying || token !== macroPlayToken) return;
            if (copy.type === "tap") dispatchTap(copy.x, copy.y);
            else if (copy.type === "scroll") dispatchScroll(copy);
          }; }(step), r * Math.max(0, Math.min(15000, Number(step.repeatInterval) || 0)));
          macroTimers.push(actionTimer);
        }
        if (step.type === "wait") {
          var waitTimer = window.setTimeout(function () { next(); }, Math.max(0, Math.min(15000, Number(step.duration) || 0)));
          macroTimers.push(waitTimer); return;
        }
        var gap = Math.max(0, Math.min(15000, Number(step.repeatInterval) || 0));
        var after = window.setTimeout(next, (repeats - 1) * gap + 20);
        macroTimers.push(after);
      }, Math.min(15000, step.delay));
      macroTimers.push(timer);
    }
    next();
  }

  macroAddTapButton.addEventListener("click", function () { addManualStep("tap"); });
  macroAddScrollButton.addEventListener("click", function () { addManualStep("scroll"); });
  macroAddWaitButton.addEventListener("click", function () { addManualStep("wait"); });
  macroPlayButton.addEventListener("click", function () {
    if (macroPlaying) stopMacroPlayback();
    else playMacro();
  });
  macroClearButton.addEventListener("click", function () {
    stopMacroPlayback();
    macroSteps = [];
    try { localStorage.removeItem(macroKey); } catch (ignored) {}
    updateMacroUi();
  });

  loadMacro();
  updateMacroUi();

  toolsToggle.addEventListener("click", function () {
    openMenu(toolsToggle.getAttribute("aria-expanded") !== "true");
  });
  macroStageDone.addEventListener("click", function () {
    macroPickingStep = -1;
    persistMacro();
    updateMacroUi();
    openMenu(true);
  });
  mouseToggle.addEventListener("click", function () { setMouseMode(!mouseMode); });
  keyboardButton.addEventListener("click", function () {
    var target = lastTextTarget && lastTextTarget.isConnected ? lastTextTarget : keyboardSink;
    try { target.focus({ preventScroll: true }); } catch (ignored) { target.focus(); }
    if (target === keyboardSink) {
      try { window.APKForgeTextInput.onFocus("TEXTAREA", "", false); } catch (ignored) {}
    }
  });
  document.addEventListener("focusin", function (event) {
    var target = event.target;
    if (isToolElement(target)) return;
    if (target && (target.isContentEditable || /^(INPUT|TEXTAREA|SELECT)$/.test(target.tagName || ""))) {
      lastTextTarget = target;
    }
  }, true);

  function dispatchMouse(type, x, y, target, pointer) {
    if (!target || isToolElement(target)) return;
    var common = { bubbles: true, cancelable: true, composed: true, view: window,
      clientX: x, clientY: y, screenX: x, screenY: y, button: 0, buttons: type === "mouseup" || type === "click" ? 0 : 1 };
    if (pointer && window.PointerEvent) {
      var pointerType = type === "mousedown" ? "pointerdown" : type === "mouseup" ? "pointerup" :
        type === "click" ? "click" : "pointermove";
      if (pointerType !== "click") {
        var pe = new PointerEvent(pointerType, Object.assign({}, common, { pointerId: 1, pointerType: "mouse", isPrimary: true }));
        target.dispatchEvent(pe);
      }
    }
    var mouseEvent = new MouseEvent(type, common);
    target.dispatchEvent(mouseEvent);
  }

  document.addEventListener("touchstart", function (event) {
    if (!mouseMode || event.touches.length !== 1 || isToolElement(event.target)) return;
    var touch = event.touches[0];
    var target = document.elementFromPoint(touch.clientX, touch.clientY);
    if (!target || isToolElement(target)) return;
    mouseContact = { x: touch.clientX, y: touch.clientY, target: target };
    cursor.style.left = touch.clientX + "px";
    cursor.style.top = touch.clientY + "px";
    event.preventDefault();
    dispatchMouse("mousedown", touch.clientX, touch.clientY, target, false);
  }, { capture: true, passive: false });
  document.addEventListener("touchmove", function (event) {
    if (!mouseMode || !mouseContact || event.touches.length !== 1) return;
    var touch = event.touches[0];
    mouseContact.x = touch.clientX;
    mouseContact.y = touch.clientY;
    cursor.style.left = touch.clientX + "px";
    cursor.style.top = touch.clientY + "px";
    event.preventDefault();
    dispatchMouse("mousemove", touch.clientX, touch.clientY,
      document.elementFromPoint(touch.clientX, touch.clientY), false);
  }, { capture: true, passive: false });
  document.addEventListener("touchend", function (event) {
    if (!mouseMode || !mouseContact) return;
    var point = mouseContact;
    var touch = event.changedTouches[0];
    var x = touch ? touch.clientX : point.x, y = touch ? touch.clientY : point.y;
    var target = document.elementFromPoint(x, y) || point.target;
    event.preventDefault();
    dispatchMouse("mouseup", x, y, target, false);
    dispatchMouse("click", x, y, target, false);
    mouseContact = null;
  }, { capture: true, passive: false });
  document.addEventListener("touchcancel", function () { mouseContact = null; }, { capture: true, passive: true });
  document.addEventListener("click", function (event) {
    if (macroPickingStep < 0 || isToolElement(event.target)) return;
    event.preventDefault(); event.stopPropagation();
    var picked = macroSteps[macroPickingStep];
    if (!picked) { macroPickingStep = -1; updateMacroUi(); return; }
    if (macroPickingMode === "scroll") {
      var candidate = event.target && event.target.__apkForgeTouchTarget ? event.target.__apkForgeTouchTarget : event.target;
      while (candidate && candidate !== document.body && candidate !== document.documentElement) {
        if (candidate.scrollHeight > candidate.clientHeight || candidate.scrollWidth > candidate.clientWidth) break;
        candidate = candidate.parentElement;
      }
      picked.path = candidate && candidate !== document.body && candidate !== document.documentElement ?
        elementPath(candidate) : "";
      macroStatus.textContent = picked.path ? "Nested scroll area selected" : "Page scrolling selected";
    } else {
      picked.x = event.clientX / Math.max(1, window.innerWidth);
      picked.y = event.clientY / Math.max(1, window.innerHeight);
    }
    macroPickingStep = -1; persistMacro(); updateMacroUi(); openMenu(true);
  }, true);

  scheduleRefresh();
  window.addEventListener("resize", scheduleRefresh, { passive: true });
  document.addEventListener("scroll", scheduleRefresh, { passive: true, capture: true });
  if (window.MutationObserver) {
    new MutationObserver(function (changes) {
      for (var i = 0; i < changes.length; i++) {
        var change = changes[i];
        if (change.type === "attributes" && change.target.hasAttribute("data-apkforge-touch-overlay")) continue;
        if (change.type === "childList") {
          var changedNodes = Array.prototype.slice.call(change.addedNodes).concat(
            Array.prototype.slice.call(change.removedNodes));
          if (changedNodes.length && changedNodes.every(function (n) {
            return n.nodeType !== 1 || n.hasAttribute("data-apkforge-touch-overlay");
          })) continue;
        }
        scheduleMutationRefresh();
        break;
      }
    }).observe(document.documentElement, { childList: true, subtree: true, attributes: true,
      attributeFilter: ["class", "style", "hidden", "disabled", "aria-disabled", "role"] });
  }
})();
