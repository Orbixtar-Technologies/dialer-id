#!/usr/bin/env python3
"""Generate ios/DialerID.xcodeproj/project.pbxproj from the source tree."""

from __future__ import annotations

import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP_DIR = ROOT / "DialerID"
TEST_DIR = ROOT / "DialerIDTests"
PROJ_DIR = ROOT / "DialerID.xcodeproj"


def hid(name: str) -> str:
    return hashlib.sha1(name.encode("utf-8")).hexdigest()[:24].upper()


def collect(directory: Path, suffixes: tuple[str, ...]) -> list[Path]:
    return sorted(
        path for path in directory.rglob("*") if path.is_file() and path.suffix in suffixes
    )


def file_type(path: Path) -> str:
    return {
        ".swift": "sourcecode.swift",
        ".json": "text.json",
        ".plist": "text.plist.xml",
        ".entitlements": "text.plist.entitlements",
        ".xcconfig": "text.xcconfig",
    }.get(path.suffix, "text")


def main() -> None:
    sources = collect(APP_DIR, (".swift",))
    tests = collect(TEST_DIR, (".swift",))

    ids = {
        "project": hid("project"),
        "main_group": hid("group:main"),
        "app_group": hid("group:app"),
        "test_group": hid("group:tests"),
        "products": hid("group:products"),
        "app_product": hid("ref:app"),
        "test_product": hid("ref:tests"),
        "app_target": hid("target:app"),
        "test_target": hid("target:tests"),
        "sources_phase": hid("phase:sources"),
        "resources_phase": hid("phase:resources"),
        "firebase_plist_phase": hid("phase:firebaseplist"),
        "frameworks_phase": hid("phase:frameworks"),
        "test_sources_phase": hid("phase:testsources"),
        "info": hid("ref:Info.plist"),
        "entitlements": hid("ref:entitlements"),
        "assets": hid("ref:assets"),
        "rates": hid("ref:rates"),
        "config": hid("ref:config"),
        "assets_build": hid("build:assets"),
        "rates_build": hid("build:rates"),
        "xc_proj": hid("xc:proj"),
        "xc_app": hid("xc:app"),
        "xc_test": hid("xc:test"),
        "debug_proj": hid("xc:debugproj"),
        "release_proj": hid("xc:releaseproj"),
        "debug_app": hid("xc:debugapp"),
        "release_app": hid("xc:releaseapp"),
        "debug_test": hid("xc:debugtest"),
        "release_test": hid("xc:releasetest"),
    }

    file_refs = [
        f'\t\t{ids["app_product"]} /* DialerID.app */ = {{isa = PBXFileReference; explicitFileType = wrapper.application; includeInIndex = 0; path = DialerID.app; sourceTree = BUILT_PRODUCTS_DIR; }};',
        f'\t\t{ids["test_product"]} /* DialerIDTests.xctest */ = {{isa = PBXFileReference; explicitFileType = wrapper.cfbundle; includeInIndex = 0; path = DialerIDTests.xctest; sourceTree = BUILT_PRODUCTS_DIR; }};',
        f'\t\t{ids["info"]} /* Info.plist */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.xml; path = Info.plist; sourceTree = "<group>"; }};',
        f'\t\t{ids["entitlements"]} /* DialerID.entitlements */ = {{isa = PBXFileReference; lastKnownFileType = text.plist.entitlements; path = DialerID.entitlements; sourceTree = "<group>"; }};',
        f'\t\t{ids["assets"]} /* Assets.xcassets */ = {{isa = PBXFileReference; lastKnownFileType = folder.assetcatalog; name = Assets.xcassets; path = Resources/Assets.xcassets; sourceTree = "<group>"; }};',
        f'\t\t{ids["rates"]} /* rates.json */ = {{isa = PBXFileReference; lastKnownFileType = text.json; name = rates.json; path = Resources/rates.json; sourceTree = "<group>"; }};',
        f'\t\t{ids["config"]} /* Config.xcconfig */ = {{isa = PBXFileReference; lastKnownFileType = text.xcconfig; path = Config.xcconfig; sourceTree = "<group>"; }};',
    ]
    build_files = [
        f'\t\t{ids["assets_build"]} /* Assets.xcassets in Resources */ = {{isa = PBXBuildFile; fileRef = {ids["assets"]} /* Assets.xcassets */; }};',
        f'\t\t{ids["rates_build"]} /* rates.json in Resources */ = {{isa = PBXBuildFile; fileRef = {ids["rates"]} /* rates.json */; }};',
    ]
    app_children = [
        f'\t\t\t\t{ids["info"]} /* Info.plist */,',
        f'\t\t\t\t{ids["entitlements"]} /* DialerID.entitlements */,',
        f'\t\t\t\t{ids["assets"]} /* Assets.xcassets */,',
        f'\t\t\t\t{ids["rates"]} /* rates.json */,',
    ]
    source_entries = []
    test_children = []
    test_entries = []

    for path in sources:
        rel = path.relative_to(APP_DIR).as_posix()
        ref = hid(f"ref:{rel}")
        build = hid(f"build:{rel}")
        file_refs.append(
            f'\t\t{ref} /* {path.name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; name = {path.name}; path = {rel}; sourceTree = "<group>"; }};'
        )
        build_files.append(
            f"\t\t{build} /* {path.name} in Sources */ = {{isa = PBXBuildFile; fileRef = {ref} /* {path.name} */; }};"
        )
        app_children.append(f"\t\t\t\t{ref} /* {path.name} */,")
        source_entries.append(f"\t\t\t\t{build} /* {path.name} in Sources */,")

    for path in tests:
        rel = path.relative_to(TEST_DIR).as_posix()
        ref = hid(f"tref:{rel}")
        build = hid(f"tbuild:{rel}")
        file_refs.append(
            f'\t\t{ref} /* {path.name} */ = {{isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = {rel}; sourceTree = "<group>"; }};'
        )
        build_files.append(
            f"\t\t{build} /* {path.name} in Sources */ = {{isa = PBXBuildFile; fileRef = {ref} /* {path.name} */; }};"
        )
        test_children.append(f"\t\t\t\t{ref} /* {path.name} */,")
        test_entries.append(f"\t\t\t\t{build} /* {path.name} in Sources */,")

    pbx = f"""// !$*UTF8*$!
{{
	archiveVersion = 1;
	classes = {{
	}};
	objectVersion = 56;
	objects = {{

/* Begin PBXBuildFile section */
{chr(10).join(build_files)}
/* End PBXBuildFile section */

/* Begin PBXFileReference section */
{chr(10).join(file_refs)}
/* End PBXFileReference section */

/* Begin PBXFrameworksBuildPhase section */
		{ids["frameworks_phase"]} /* Frameworks */ = {{
			isa = PBXFrameworksBuildPhase;
			buildActionMask = 2147483647;
			files = (
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXFrameworksBuildPhase section */

/* Begin PBXGroup section */
		{ids["main_group"]} = {{
			isa = PBXGroup;
			children = (
				{ids["app_group"]} /* DialerID */,
				{ids["test_group"]} /* DialerIDTests */,
				{ids["config"]} /* Config.xcconfig */,
				{ids["products"]} /* Products */,
			);
			sourceTree = "<group>";
		}};
		{ids["products"]} /* Products */ = {{
			isa = PBXGroup;
			children = (
				{ids["app_product"]} /* DialerID.app */,
				{ids["test_product"]} /* DialerIDTests.xctest */,
			);
			name = Products;
			sourceTree = "<group>";
		}};
		{ids["app_group"]} /* DialerID */ = {{
			isa = PBXGroup;
			children = (
{chr(10).join(app_children)}
			);
			path = DialerID;
			sourceTree = "<group>";
		}};
		{ids["test_group"]} /* DialerIDTests */ = {{
			isa = PBXGroup;
			children = (
{chr(10).join(test_children)}
			);
			path = DialerIDTests;
			sourceTree = "<group>";
		}};
/* End PBXGroup section */

/* Begin PBXNativeTarget section */
		{ids["app_target"]} /* DialerID */ = {{
			isa = PBXNativeTarget;
			buildConfigurationList = {ids["xc_app"]} /* Build configuration list for PBXNativeTarget "DialerID" */;
			buildPhases = (
				{ids["sources_phase"]} /* Sources */,
				{ids["frameworks_phase"]} /* Frameworks */,
				{ids["resources_phase"]} /* Resources */,
				{ids["firebase_plist_phase"]} /* Copy GoogleService-Info.plist if present */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = DialerID;
			productName = DialerID;
			productReference = {ids["app_product"]} /* DialerID.app */;
			productType = "com.apple.product-type.application";
		}};
		{ids["test_target"]} /* DialerIDTests */ = {{
			isa = PBXNativeTarget;
			buildConfigurationList = {ids["xc_test"]} /* Build configuration list for PBXNativeTarget "DialerIDTests" */;
			buildPhases = (
				{ids["test_sources_phase"]} /* Sources */,
			);
			buildRules = (
			);
			dependencies = (
			);
			name = DialerIDTests;
			productName = DialerIDTests;
			productReference = {ids["test_product"]} /* DialerIDTests.xctest */;
			productType = "com.apple.product-type.bundle.unit-test";
		}};
/* End PBXNativeTarget section */

/* Begin PBXProject section */
		{ids["project"]} /* Project object */ = {{
			isa = PBXProject;
			attributes = {{
				BuildIndependentTargetsInParallel = 1;
				LastSwiftUpdateCheck = 1600;
				LastUpgradeCheck = 1600;
			}};
			buildConfigurationList = {ids["xc_proj"]} /* Build configuration list for PBXProject "DialerID" */;
			compatibilityVersion = "Xcode 14.0";
			developmentRegion = en;
			hasScannedForEncodings = 0;
			knownRegions = (
				en,
				Base,
			);
			mainGroup = {ids["main_group"]};
			productRefGroup = {ids["products"]} /* Products */;
			projectDirPath = "";
			projectRoot = "";
			targets = (
				{ids["app_target"]} /* DialerID */,
				{ids["test_target"]} /* DialerIDTests */,
			);
		}};
/* End PBXProject section */

/* Begin PBXResourcesBuildPhase section */
		{ids["resources_phase"]} /* Resources */ = {{
			isa = PBXResourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
				{ids["assets_build"]} /* Assets.xcassets in Resources */,
				{ids["rates_build"]} /* rates.json in Resources */,
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXResourcesBuildPhase section */

/* Begin PBXShellScriptBuildPhase section */
		{ids["firebase_plist_phase"]} /* Copy GoogleService-Info.plist if present */ = {{
			isa = PBXShellScriptBuildPhase;
			alwaysOutOfDate = 1;
			buildActionMask = 2147483647;
			files = (
			);
			inputPaths = (
			);
			name = "Copy GoogleService-Info.plist if present";
			outputPaths = (
			);
			runOnlyForDeploymentPostprocessing = 0;
			shellPath = /bin/sh;
			shellScript = "if [ -f \\"$SRCROOT/DialerID/GoogleService-Info.plist\\" ]; then\\n  cp \\"$SRCROOT/DialerID/GoogleService-Info.plist\\" \\"$TARGET_BUILD_DIR/$UNLOCALIZED_RESOURCES_FOLDER_PATH/GoogleService-Info.plist\\"\\nfi\\n";
		}};
/* End PBXShellScriptBuildPhase section */

/* Begin PBXSourcesBuildPhase section */
		{ids["sources_phase"]} /* Sources */ = {{
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
{chr(10).join(source_entries)}
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
		{ids["test_sources_phase"]} /* Sources */ = {{
			isa = PBXSourcesBuildPhase;
			buildActionMask = 2147483647;
			files = (
{chr(10).join(test_entries)}
			);
			runOnlyForDeploymentPostprocessing = 0;
		}};
/* End PBXSourcesBuildPhase section */

/* Begin XCBuildConfiguration section */
		{ids["debug_proj"]} /* Debug */ = {{
			isa = XCBuildConfiguration;
			baseConfigurationReference = {ids["config"]} /* Config.xcconfig */;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				COPY_PHASE_STRIP = NO;
				DEBUG_INFORMATION_FORMAT = dwarf;
				ENABLE_TESTABILITY = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				ONLY_ACTIVE_ARCH = YES;
				SDKROOT = iphoneos;
				SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG;
				SWIFT_OPTIMIZATION_LEVEL = "-Onone";
				SWIFT_VERSION = 5.0;
			}};
			name = Debug;
		}};
		{ids["release_proj"]} /* Release */ = {{
			isa = XCBuildConfiguration;
			baseConfigurationReference = {ids["config"]} /* Config.xcconfig */;
			buildSettings = {{
				ALWAYS_SEARCH_USER_PATHS = NO;
				CLANG_ENABLE_MODULES = YES;
				CLANG_ENABLE_OBJC_ARC = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				SDKROOT = iphoneos;
				SWIFT_COMPILATION_MODE = wholemodule;
				SWIFT_VERSION = 5.0;
				VALIDATE_PRODUCT = YES;
			}};
			name = Release;
		}};
		{ids["debug_app"]} /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_ENTITLEMENTS = DialerID/DialerID.entitlements;
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				ENABLE_TESTABILITY = YES;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = DialerID/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = "$(inherited) @executable_path/Frameworks";
				MARKETING_VERSION = 1.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.dialerid.app;
				PRODUCT_NAME = DialerID;
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
			}};
			name = Debug;
		}};
		{ids["release_app"]} /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;
				CODE_SIGN_ENTITLEMENTS = DialerID/DialerID.entitlements;
				CODE_SIGN_STYLE = Automatic;
				CURRENT_PROJECT_VERSION = 1;
				GENERATE_INFOPLIST_FILE = NO;
				INFOPLIST_FILE = DialerID/Info.plist;
				LD_RUNPATH_SEARCH_PATHS = "$(inherited) @executable_path/Frameworks";
				MARKETING_VERSION = 1.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.dialerid.app;
				PRODUCT_NAME = DialerID;
				SUPPORTED_PLATFORMS = "iphoneos iphonesimulator";
				SUPPORTS_MACCATALYST = NO;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
			}};
			name = Release;
		}};
		{ids["debug_test"]} /* Debug */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				BUNDLE_LOADER = "$(TEST_HOST)";
				CODE_SIGN_STYLE = Automatic;
				GENERATE_INFOPLIST_FILE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.dialerid.app.tests;
				PRODUCT_NAME = DialerIDTests;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
				TEST_HOST = "$(BUILT_PRODUCTS_DIR)/DialerID.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/DialerID";
			}};
			name = Debug;
		}};
		{ids["release_test"]} /* Release */ = {{
			isa = XCBuildConfiguration;
			buildSettings = {{
				BUNDLE_LOADER = "$(TEST_HOST)";
				CODE_SIGN_STYLE = Automatic;
				GENERATE_INFOPLIST_FILE = YES;
				IPHONEOS_DEPLOYMENT_TARGET = 16.0;
				PRODUCT_BUNDLE_IDENTIFIER = com.dialerid.app.tests;
				PRODUCT_NAME = DialerIDTests;
				SWIFT_VERSION = 5.0;
				TARGETED_DEVICE_FAMILY = 1;
				TEST_HOST = "$(BUILT_PRODUCTS_DIR)/DialerID.app/$(BUNDLE_EXECUTABLE_FOLDER_PATH)/DialerID";
			}};
			name = Release;
		}};
/* End XCBuildConfiguration section */

/* Begin XCConfigurationList section */
		{ids["xc_proj"]} /* Build configuration list for PBXProject "DialerID" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				{ids["debug_proj"]} /* Debug */,
				{ids["release_proj"]} /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
		{ids["xc_app"]} /* Build configuration list for PBXNativeTarget "DialerID" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				{ids["debug_app"]} /* Debug */,
				{ids["release_app"]} /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
		{ids["xc_test"]} /* Build configuration list for PBXNativeTarget "DialerIDTests" */ = {{
			isa = XCConfigurationList;
			buildConfigurations = (
				{ids["debug_test"]} /* Debug */,
				{ids["release_test"]} /* Release */,
			);
			defaultConfigurationIsVisible = 0;
			defaultConfigurationName = Release;
		}};
/* End XCConfigurationList section */
	}};
	rootObject = {ids["project"]} /* Project object */;
}}
"""

    PROJ_DIR.mkdir(parents=True, exist_ok=True)
    (PROJ_DIR / "project.pbxproj").write_text(pbx.replace("\r\n", "\n"), encoding="utf-8")
    workspace = PROJ_DIR / "project.xcworkspace"
    workspace.mkdir(exist_ok=True)
    (workspace / "contents.xcworkspacedata").write_text(
        """<?xml version="1.0" encoding="UTF-8"?>
<Workspace
   version = "1.0">
   <FileRef
      location = "self:">
   </FileRef>
</Workspace>
""",
        encoding="utf-8",
    )
    print(f"Wrote {PROJ_DIR / 'project.pbxproj'} ({len(sources)} app sources, {len(tests)} tests)")
    write_shared_scheme(ids["app_target"])


def write_shared_scheme(app_target_id: str) -> None:
    scheme_dir = PROJ_DIR / "xcshareddata" / "xcschemes"
    scheme_dir.mkdir(parents=True, exist_ok=True)
    (scheme_dir / "DialerID.xcscheme").write_text(
        f"""<?xml version="1.0" encoding="UTF-8"?>
<Scheme
   LastUpgradeVersion = "1500"
   version = "1.7">
   <BuildAction
      parallelizeBuildables = "YES"
      buildImplicitDependencies = "YES">
      <BuildActionEntries>
         <BuildActionEntry
            buildForTesting = "YES"
            buildForRunning = "YES"
            buildForProfiling = "YES"
            buildForArchiving = "YES"
            buildForAnalyzing = "YES">
            <BuildableReference
               BuildableIdentifier = "primary"
               BlueprintIdentifier = "{app_target_id}"
               BuildableName = "DialerID.app"
               BlueprintName = "DialerID"
               ReferencedContainer = "container:DialerID.xcodeproj">
            </BuildableReference>
         </BuildActionEntry>
      </BuildActionEntries>
   </BuildAction>
   <TestAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      shouldUseLaunchSchemeArgsEnv = "YES"
      shouldAutocreateTestPlan = "YES">
   </TestAction>
   <LaunchAction
      buildConfiguration = "Debug"
      selectedDebuggerIdentifier = "Xcode.DebuggerFoundation.Debugger.LLDB"
      selectedLauncherIdentifier = "Xcode.DebuggerFoundation.Launcher.LLDB"
      launchStyle = "0"
      useCustomWorkingDirectory = "NO"
      ignoresPersistentStateOnLaunch = "NO"
      debugDocumentVersioning = "YES"
      debugServiceExtension = "internal"
      allowLocationSimulation = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{app_target_id}"
            BuildableName = "DialerID.app"
            BlueprintName = "DialerID"
            ReferencedContainer = "container:DialerID.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </LaunchAction>
   <ProfileAction
      buildConfiguration = "Release"
      shouldUseLaunchSchemeArgsEnv = "YES"
      savedToolIdentifier = ""
      useCustomWorkingDirectory = "NO"
      debugDocumentVersioning = "YES">
      <BuildableProductRunnable
         runnableDebuggingMode = "0">
         <BuildableReference
            BuildableIdentifier = "primary"
            BlueprintIdentifier = "{app_target_id}"
            BuildableName = "DialerID.app"
            BlueprintName = "DialerID"
            ReferencedContainer = "container:DialerID.xcodeproj">
         </BuildableReference>
      </BuildableProductRunnable>
   </ProfileAction>
   <AnalyzeAction
      buildConfiguration = "Debug">
   </AnalyzeAction>
   <ArchiveAction
      buildConfiguration = "Release"
      revealArchiveInOrganizer = "YES">
   </ArchiveAction>
</Scheme>
""",
        encoding="utf-8",
    )


if __name__ == "__main__":
    main()
