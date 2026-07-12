import uuid
from pathlib import Path

root = Path(__file__).parent

swift_files = [
    "TwoStrokeCalcIOS/App/TwoStrokeCalcIOSApp.swift",
    "TwoStrokeCalcIOS/App/AppState.swift",
    "TwoStrokeCalcIOS/Theme/AppTheme.swift",
    "TwoStrokeCalcIOS/Localization/S.swift",
    "TwoStrokeCalcIOS/Components/CalculatorComponents.swift",
    "TwoStrokeCalcIOS/Components/SettingsComponents.swift",
    "TwoStrokeCalcIOS/Components/PortTimingDiagramView.swift",
    "TwoStrokeCalcIOS/Components/ExhaustPipeDiagramView.swift",
    "TwoStrokeCalcIOS/Components/ExhaustPortGeometryDiagramView.swift",
    "TwoStrokeCalcIOS/Components/GearChartView.swift",
    "TwoStrokeCalcIOS/Navigation/RootView.swift",
    "TwoStrokeCalcIOS/Walkthrough/WalkthroughOverlay.swift",
    "TwoStrokeCalcIOS/Walkthrough/WalkthroughCoachmarks.swift",
    "TwoStrokeCalcIOS/Screens/OnboardingScreens.swift",
    "TwoStrokeCalcIOS/Screens/SettingsScreens.swift",
    "TwoStrokeCalcIOS/Screens/SettingsContent.swift",
    "TwoStrokeCalcIOS/Screens/SettingsDetailViews.swift",
    "TwoStrokeCalcIOS/Screens/AdminPanelViewModel.swift",
    "TwoStrokeCalcIOS/Screens/Auth/AuthScreens.swift",
    "TwoStrokeCalcIOS/Screens/Calculators/CalculatorScreen.swift",
    "TwoStrokeCalcIOS/Screens/Calculators/CalculatorOverviewScreen.swift",
    "TwoStrokeCalcIOS/Screens/Calculators/CalculatorDetailView.swift",
    "TwoStrokeCalcIOS/Screens/Calculators/AllCalculatorScreens.swift",
    "TwoStrokeCalcIOS/Screens/Vehicles/VehiclesScreens.swift",
    "TwoStrokeCalcIOS/Screens/Vehicles/VehicleDetailScreen.swift",
    "TwoStrokeCalcIOS/Screens/Vehicles/AddVehicleDialog.swift",
    "TwoStrokeCalcIOS/Screens/Vehicles/VehicleMapper.swift",
    "TwoStrokeCalcIOS/Platform/FirebaseBootstrap.swift",
    "TwoStrokeCalcIOS/Platform/CatalogBootstrap.swift",
    "TwoStrokeCalcIOS/Platform/SharedKitBridge.swift",
    "TwoStrokeCalcIOS/Platform/StoreKitService.swift",
    "TwoStrokeCalcIOS/Platform/GoogleSignInService.swift",
    "TwoStrokeCalcIOS/Platform/NotificationService.swift",
    "TwoStrokeCalcIOS/UI/ContentView.swift",
]

resource_files = [
    "TwoStrokeCalcIOS/Resources/GoogleService-Info.plist",
    "TwoStrokeCalcIOS/Resources/PrivacyInfo.xcprivacy",
    "TwoStrokeCalcIOS/Resources/vehicle_catalog.json",
]

firebase_packages = [
    ("FirebaseCore", "B90000010000000000000001"),
    ("FirebaseAuth", "B90000010000000000000002"),
    ("FirebaseFirestore", "B90000010000000000000003"),
    ("FirebaseStorage", "B90000010000000000000004"),
]

google_signin_package = ("GoogleSignIn", "C90000010000000000000001")
google_signin_pkg_ref = "C80000010000000000000001"

def uid():
    return uuid.uuid4().hex[:24].upper()

file_refs = {f: uid() for f in swift_files + resource_files}
build_files_swift = {f: uid() for f in swift_files}
build_files_resources = {f: uid() for f in resource_files}

target = "A50000010000000000000001"
project = "A70000010000000000000001"
prod_ref = "A20000010000000000000010"
sources_phase = "A50000010000000000000003"
frameworks_phase = "A30000010000000000000001"
resources_phase = "A50000010000000000000004"
shell_phase = "A50000010000000000000002"
proj_cfg_list = "A60000010000000000000001"
target_cfg_list = "A60000010000000000000003"
main_group = "A40000010000000000000001"
prod_group = "A40000010000000000000003"
ios_group = "A40000010000000000000002"
firebase_pkg_ref = "B80000010000000000000001"

folder_keys = {"TwoStrokeCalcIOS", "TwoStrokeCalcIOS/Resources"}
for f in swift_files + resource_files:
    parts = Path(f).parts[:-1]
    for i in range(1, len(parts) + 1):
        folder_keys.add("/".join(parts[:i]))

group_ids = {"TwoStrokeCalcIOS": ios_group, "Products": prod_group, "TwoStrokeCalcIOS/Resources": uid()}
for key in folder_keys:
    if key not in group_ids:
        group_ids[key] = uid()

lines = [
    "// !$*UTF8*$!",
    "{",
    "\tarchiveVersion = 1;",
    "\tclasses = {};",
    "\tobjectVersion = 56;",
    "\tobjects = {",
    "",
    "/* Begin PBXBuildFile section */",
]
for f in swift_files:
    name = Path(f).name
    lines.append(f"\t\t{build_files_swift[f]} /* {name} in Sources */ = {{isa = PBXBuildFile; fileRef = {file_refs[f]} /* {name} */; }};")
for f in resource_files:
    name = Path(f).name
    lines.append(f"\t\t{build_files_resources[f]} /* {name} in Resources */ = {{isa = PBXBuildFile; fileRef = {file_refs[f]} /* {name} */; }};")
lines += ["/* End PBXBuildFile section */", "", "/* Begin PBXFileReference section */"]
lines.append(f"\t\t{prod_ref} /* TwoStrokeCalcIOS.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = TwoStrokeCalcIOS.app; sourceTree = BUILT_PRODUCTS_DIR; }};")
for f in swift_files:
    name = Path(f).name
    lines.append(f"\t\t{file_refs[f]} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {name}; sourceTree = \"<group>\"; }};")
for f in resource_files:
    name = Path(f).name
    if name.endswith(".plist"):
        file_type = "text.plist.xml"
    elif name.endswith(".json"):
        file_type = "text.json"
    else:
        file_type = "text.xml"
    lines.append(f"\t\t{file_refs[f]} /* {name} */ = {{isa = PBXFileReference; lastKnownFileType = {file_type}; path = {name}; sourceTree = \"<group>\"; }};")
lines += ["/* End PBXFileReference section */", "", "/* Begin PBXFrameworksBuildPhase section */",
          f"\t\t{frameworks_phase} /* Frameworks */ = {{isa = PBXFrameworksBuildPhase; buildActionMask = 2147483647; files = (); runOnlyForDeploymentPostprocessing = 0; }};",
          "/* End PBXFrameworksBuildPhase section */", "", "/* Begin PBXGroup section */",
          f"\t\t{main_group} = {{isa = PBXGroup; children = ({ios_group} /* TwoStrokeCalcIOS */, {prod_group} /* Products */); sourceTree = \"<group>\"; }};",
          f"\t\t{prod_group} /* Products */ = {{isa = PBXGroup; children = ({prod_ref} /* TwoStrokeCalcIOS.app */); name = Products; sourceTree = \"<group>\"; }};"]

for key in sorted(folder_keys, key=lambda k: (k.count("/"), k)):
    gid = group_ids[key]
    children = []
    for other in sorted(folder_keys):
        if other.startswith(key + "/") and other.count("/") == key.count("/") + 1:
            children.append(f"{group_ids[other]} /* {Path(other).name} */")
    for f in sorted(swift_files + resource_files):
        if str(Path(f).parent).replace("\\", "/") == key:
            children.append(f"{file_refs[f]} /* {Path(f).name} */")
    path_line = f"path = TwoStrokeCalcIOS;" if key == "TwoStrokeCalcIOS" else f"path = {Path(key).name};"
    child_block = "\n".join(f"\t\t\t\t{c}," for c in children)
    lines.append(f"\t\t{gid} /* {Path(key).name} */ = {{\n\t\t\tisa = PBXGroup;\n\t\t\tchildren = (\n{child_block}\n\t\t\t);\n\t\t\t{path_line}\n\t\t\tsourceTree = \"<group>\";\n\t\t}};")

pkg_deps = ", ".join(pid for _, pid in firebase_packages + [google_signin_package])
lines += [
    "/* End PBXGroup section */",
    "",
    "/* Begin PBXNativeTarget section */",
    f"\t\t{target} /* TwoStrokeCalcIOS */ = {{isa = PBXNativeTarget; buildConfigurationList = {target_cfg_list}; buildPhases = ({shell_phase} /* Compile Kotlin Framework */, {sources_phase} /* Sources */, {frameworks_phase} /* Frameworks */, {resources_phase} /* Resources */); buildRules = (); dependencies = (); name = TwoStrokeCalcIOS; packageProductDependencies = ({pkg_deps}); productName = TwoStrokeCalcIOS; productReference = {prod_ref}; productType = \"com.apple.product-type.application\"; }};",
    "/* End PBXNativeTarget section */",
    "",
    "/* Begin PBXProject section */",
    f"\t\t{project} /* Project object */ = {{isa = PBXProject; attributes = {{BuildIndependentTargetsInParallel = 1; LastSwiftUpdateCheck = 1500; LastUpgradeCheck = 1500;}}; buildConfigurationList = {proj_cfg_list}; compatibilityVersion = \"Xcode 14.0\"; developmentRegion = de; hasScannedForEncodings = 0; knownRegions = (de, en, Base); mainGroup = {main_group}; packageReferences = ({firebase_pkg_ref} /* firebase */, {google_signin_pkg_ref} /* GoogleSignIn */); productRefGroup = {prod_group}; projectDirPath = \"\"; projectRoot = \"\"; targets = ({target}); }};",
    "/* End PBXProject section */",
    "",
    f"/* Begin PBXResourcesBuildPhase section */\n\t\t{resources_phase} /* Resources */ = {{isa = PBXResourcesBuildPhase; buildActionMask = 2147483647; files = (",
]
for f in resource_files:
    lines.append(f"\t\t\t\t{build_files_resources[f]} /* {Path(f).name} in Resources */,")
lines += [
    "\t\t\t); runOnlyForDeploymentPostprocessing = 0; };",
    "/* End PBXResourcesBuildPhase section */",
    "",
    f"/* Begin PBXShellScriptBuildPhase section */\n\t\t{shell_phase} /* Compile Kotlin Framework */ = {{isa = PBXShellScriptBuildPhase; alwaysOutOfDate = 1; buildActionMask = 2147483647; files = (); inputPaths = (); outputPaths = (); name = \"Compile Kotlin Framework\"; runOnlyForDeploymentPostprocessing = 0; shellPath = /bin/sh; shellScript = \"if [ \\\"YES\\\" = \\\"$OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED\\\" ]; then exit 0; fi\\ncd \\\"$SRCROOT/..\\\"\\n./gradlew :shared:embedAndSignAppleFrameworkForXcode\\n\"; }};\n/* End PBXShellScriptBuildPhase section */",
    "",
    "/* Begin PBXSourcesBuildPhase section */",
    f"\t\t{sources_phase} /* Sources */ = {{isa = PBXSourcesBuildPhase; buildActionMask = 2147483647; files = (",
]
for f in swift_files:
    lines.append(f"\t\t\t\t{build_files_swift[f]} /* {Path(f).name} in Sources */,")
lines += [
    "\t\t\t); runOnlyForDeploymentPostprocessing = 0; };",
    "/* End PBXSourcesBuildPhase section */",
    "",
    "/* Begin XCBuildConfiguration section */",
    "\t\tA80000010000000000000001 /* Debug */ = {isa = XCBuildConfiguration; buildSettings = {ALWAYS_SEARCH_USER_PATHS = NO; CLANG_ENABLE_MODULES = YES; CODE_SIGN_STYLE = Automatic; CURRENT_PROJECT_VERSION = 1; DEVELOPMENT_TEAM = \"\"; ENABLE_USER_SCRIPT_SANDBOXING = NO; GENERATE_INFOPLIST_FILE = YES; INFOPLIST_KEY_CFBundleDisplayName = \"2-Stroke Calc\"; INFOPLIST_KEY_UIApplicationSceneManifest_Generation = YES; INFOPLIST_KEY_UILaunchScreen_Generation = YES; INFOPLIST_KEY_CFBundleURLTypes = ({CFBundleURLName = GoogleSignIn; CFBundleURLSchemes = (\"com.googleusercontent.apps.944061414373-h2qmtb8j72m37sd14k2nbania9f0d34h\");}); IPHONEOS_DEPLOYMENT_TARGET = 16.0; LD_RUNPATH_SEARCH_PATHS = (\"$(inherited)\", \"@executable_path/Frameworks\"); MARKETING_VERSION = 1.0; PRODUCT_BUNDLE_IDENTIFIER = com.simplestsoft.twostrokecalc.ios; PRODUCT_NAME = \"$(TARGET_NAME)\"; SWIFT_EMIT_LOC_STRINGS = YES; SWIFT_VERSION = 5.0; TARGETED_DEVICE_FAMILY = \"1,2\";}; name = Debug;};",
    "\t\tA80000010000000000000002 /* Release */ = {isa = XCBuildConfiguration; buildSettings = {ALWAYS_SEARCH_USER_PATHS = NO; CLANG_ENABLE_MODULES = YES; CODE_SIGN_STYLE = Automatic; CURRENT_PROJECT_VERSION = 1; DEVELOPMENT_TEAM = \"\"; ENABLE_USER_SCRIPT_SANDBOXING = NO; GENERATE_INFOPLIST_FILE = YES; INFOPLIST_KEY_CFBundleDisplayName = \"2-Stroke Calc\"; INFOPLIST_KEY_UIApplicationSceneManifest_Generation = YES; INFOPLIST_KEY_UILaunchScreen_Generation = YES; INFOPLIST_KEY_CFBundleURLTypes = ({CFBundleURLName = GoogleSignIn; CFBundleURLSchemes = (\"com.googleusercontent.apps.944061414373-h2qmtb8j72m37sd14k2nbania9f0d34h\");}); IPHONEOS_DEPLOYMENT_TARGET = 16.0; LD_RUNPATH_SEARCH_PATHS = (\"$(inherited)\", \"@executable_path/Frameworks\"); MARKETING_VERSION = 1.0; PRODUCT_BUNDLE_IDENTIFIER = com.simplestsoft.twostrokecalc.ios; PRODUCT_NAME = \"$(TARGET_NAME)\"; SWIFT_EMIT_LOC_STRINGS = YES; SWIFT_VERSION = 5.0; TARGETED_DEVICE_FAMILY = \"1,2\";}; name = Release;};",
    "\t\tA80000010000000000000003 /* Debug */ = {isa = XCBuildConfiguration; buildSettings = {CODE_SIGN_STYLE = Automatic; DEVELOPMENT_TEAM = \"\"; ENABLE_USER_SCRIPT_SANDBOXING = NO; FRAMEWORK_SEARCH_PATHS = (\"$(inherited)\", \"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)\"); OTHER_LDFLAGS = (\"$(inherited)\", \"-framework\", sharedKit); SWIFT_VERSION = 5.0;}; name = Debug;};",
    "\t\tA80000010000000000000004 /* Release */ = {isa = XCBuildConfiguration; buildSettings = {CODE_SIGN_STYLE = Automatic; DEVELOPMENT_TEAM = \"\"; ENABLE_USER_SCRIPT_SANDBOXING = NO; FRAMEWORK_SEARCH_PATHS = (\"$(inherited)\", \"$(SRCROOT)/../shared/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)\"); OTHER_LDFLAGS = (\"$(inherited)\", \"-framework\", sharedKit); SWIFT_VERSION = 5.0;}; name = Release;};",
    "/* End XCBuildConfiguration section */",
    "",
    "/* Begin XCConfigurationList section */",
    f"\t\t{proj_cfg_list} = {{isa = XCConfigurationList; buildConfigurations = (A80000010000000000000001 /* Debug */, A80000010000000000000002 /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};",
    f"\t\t{target_cfg_list} = {{isa = XCConfigurationList; buildConfigurations = (A80000010000000000000003 /* Debug */, A80000010000000000000004 /* Release */); defaultConfigurationIsVisible = 0; defaultConfigurationName = Release; }};",
    "/* End XCConfigurationList section */",
    "",
    "/* Begin XCRemoteSwiftPackageReference section */",
    f"\t\t{firebase_pkg_ref} /* XCRemoteSwiftPackageReference \"firebase-ios-sdk\" */ = {{isa = XCRemoteSwiftPackageReference; repositoryURL = \"https://github.com/firebase/firebase-ios-sdk\"; requirement = {{kind = upToNextMajorVersion; minimumVersion = 11.0.0;}}; }};",
    f"\t\t{google_signin_pkg_ref} /* XCRemoteSwiftPackageReference \"GoogleSignIn-iOS\" */ = {{isa = XCRemoteSwiftPackageReference; repositoryURL = \"https://github.com/google/GoogleSignIn-iOS\"; requirement = {{kind = upToNextMajorVersion; minimumVersion = 8.0.0;}}; }};",
    "/* End XCRemoteSwiftPackageReference section */",
    "",
    "/* Begin XCSwiftPackageProductDependency section */",
]
for name, pid in firebase_packages:
    lines.append(f"\t\t{pid} /* {name} */ = {{isa = XCSwiftPackageProductDependency; package = {firebase_pkg_ref}; productName = {name}; }};")
name, pid = google_signin_package
lines.append(f"\t\t{pid} /* {name} */ = {{isa = XCSwiftPackageProductDependency; package = {google_signin_pkg_ref}; productName = {name}; }};")
lines += [
    "/* End XCSwiftPackageProductDependency section */",
    "\t};",
    f"\trootObject = {project};",
    "}",
]

out = root / "TwoStrokeCalcIOS.xcodeproj" / "project.pbxproj"
out.write_text("\n".join(lines) + "\n", encoding="utf-8")
print(f"Wrote {out} with {len(swift_files)} Swift sources, {len(resource_files)} resources")
