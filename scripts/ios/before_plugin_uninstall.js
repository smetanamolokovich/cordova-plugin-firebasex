var helper = require("./helper");

module.exports = function(context) {

    try {
        // Remove the build script that was added when the plugin was installed.
        var xcodeProjectPath = helper.getXcodeProjectPath();
        helper.removeShellScriptBuildPhase(context, xcodeProjectPath);
        helper.removeGoogleTagManagerContainer(context, xcodeProjectPath);
    } catch (e) {
        // Ignore errors in non-standard Cordova environments (e.g., Meteor)
        console.log("Note: Skipping iOS build phase cleanup - " + e.message);
    }
};
