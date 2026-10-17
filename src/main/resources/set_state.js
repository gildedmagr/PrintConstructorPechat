// Runs inside Selenium executeAsyncScript(script, savedJSON, maxWaitMs).
// In constructor.min.js Side2D.deserialize -> setState -> addNextObject uses
// fabric.Image.fromObject asynchronously. c.setState's callback by itself does
// NOT mean the images were loaded onto the 2D canvases, or on 3D materials.
var json = arguments[0];
var timeoutMs = Number(arguments[1]) || 15000;
var done = arguments[arguments.length - 1];
var started = Date.now();
var completed = false;
var scheduled = null;
var state;
var expected;

function finish(result) {
    if (completed) return;
    completed = true;
    if (scheduled !== null) clearTimeout(scheduled);
    done(result);
}
function fail(reason) { finish('ERROR: ' + reason); }
function later(fn) { if (!completed) scheduled = setTimeout(fn, 70); }
function expired(reason) {
    if (Date.now() - started >= timeoutMs) {
        fail('timeout waiting for ' + reason);
        return true;
    }
    return false;
}
function verifyTexture() {
    if (completed) return;
    var missing = [];
    for (var i = 0; i < expected.length; i++) {
        if (!expected[i].length) continue;
        var material = c.preview && c.preview.sides && c.preview.sides[i];
        var texture = material && material.map;
        if (!material || material.userData !== true || !texture ||
            !texture.image || !texture.image.complete || !texture.image.naturalWidth) {
            missing.push(i + 1);
        }
    }
    if (!missing.length) {
        try { c.preview.render(); finish('OK'); }
        catch (e) { fail('3D render update: ' + String(e.message || e)); }
    } else if (!expired('3D texture for side(s) ' + missing.join(','))) {
        later(verifyTexture);
    }
}
function waitForDesign() {
    if (completed) return;
    try {
        var missing = [];
        var sides = c.sides || [];
        if (sides.length !== expected.length) {
            missing.push('side count ' + sides.length + '/' + expected.length);
        }
        for (var i = 0; i < expected.length; i++) {
            var side = sides[i];
            if (!side) continue;
            var items = side.elements || [];
            var total = expected[i].length;
            if (items.length !== total ||
                (side.history && typeof side.history.isLocked === 'function' && side.history.isLocked())) {
                missing.push('side ' + (i + 1) + ' objects ' + items.length + '/' + total);
                continue;
            }
            for (var j = 0; j < items.length; j++) {
                var object = items[j].object;
                if (!object || object.type !== 'image') continue;
                var element = (typeof object.getElement === 'function') ? object.getElement() : object._element;
                if (!element || !element.complete || !element.naturalWidth) {
                    missing.push('image ' + (j + 1) + ' on side ' + (i + 1));
                }
            }
        }
        if (missing.length) {
            if (!expired(missing.join('; '))) later(waitForDesign);
            return;
        }
        sides.forEach(function(side) { side.canvas.renderAll(); });
        // Rebuild the texture AFTER fabric.Image.fromObject resolves.
        // The old code updated it while the 2D layer was still empty.
        c.preview.updateSideMaterials(function() {
            if (!completed) verifyTexture();
        });
    } catch (e) {
        fail('restoring print: ' + String(e && e.message || e));
    }
}
try {
    state = JSON.parse(json);
    if (!state || !Array.isArray(state.sides)) {
        fail('saved JSON has no sides');
    } else {
        expected = state.sides.map(function(side) {
            return Array.isArray(side.objects) ? side.objects : [];
        });
        c.setState(json, function() {
            if (!completed) waitForDesign();
        });
    }
} catch (e) {
    fail('setState failed: ' + String(e && e.message || e));
}
