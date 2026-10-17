// Called by Selenium executeAsyncScript(script, modelName).
// Signature in constructor.min.js: c.loadModel(name, mode, onSuccess, onError).
var modelName = arguments[0];
var seleniumCallback = arguments[arguments.length - 1];
var finished = false;
var preview = c.preview;

function finish(status) {
    if (finished) return;
    finished = true;
    seleniumCallback(status);
}

function fail(reason) {
    // Do not cache a failed load as if the requested geometry were present.
    preview.modelName = null;
    preview.isLoaded = false;
    finish('ERROR: ' + String(reason || '3D model load failed'));
}

try {
    // A prior failed attempt may have assigned modelName before it loaded a scene.
    if (preview.modelName === modelName && !preview.isLoaded) {
        preview.modelName = null;
    }

    c.loadModel(modelName, Mode.Mode3D,
        function () {
            if (preview.isLoaded === true && preview.modelName === modelName) {
                finish('OK');
            } else {
                fail('Model callback arrived without loaded 3D geometry: ' + modelName);
            }
        },
        function (error) {
            fail(error || ('Unable to load model: ' + modelName));
        }
    );
} catch (error) {
    fail(error && error.message ? error.message : error);
}
