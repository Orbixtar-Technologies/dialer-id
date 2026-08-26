Pod::Spec.new do |s|
  s.name = "linphone-sdk"
  s.version = "5.3.110"
  s.summary = "Linphone SDK used by DialerID for outbound SIP."
  s.homepage = "https://linphone.org"
  s.license = { :type => "GPL-3.0", :text => "GNU GPL 3.0" }
  s.author = { "Belledonne Communications" => "https://linphone.org" }
  s.platform = :ios, "16.0"
  s.source = { :http => "https://download.linphone.org/releases/ios/linphone-sdk-5.3.110.zip" }
  s.module_name = "linphonesw"
  s.static_framework = true
  s.vendored_frameworks = "Frameworks/*.xcframework"
  s.source_files = "linphonesw/**/*.swift"
  s.swift_version = "5.0"
  s.frameworks = %w[
    AVFoundation AudioToolbox CoreMedia VideoToolbox UIKit QuartzCore
    OpenGLES CoreGraphics CoreVideo CFNetwork Security SystemConfiguration
  ]
  s.libraries = %w[c++ iconv xml2 z resolv sqlite3]
  s.pod_target_xcconfig = {
    "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
    "DEFINES_MODULE" => "YES",
    "ENABLE_BITCODE" => "NO",
    "OTHER_LDFLAGS" => "$(inherited) -ObjC"
  }
  s.user_target_xcconfig = {
    "ENABLE_BITCODE" => "NO",
    "SWIFT_ACTIVE_COMPILATION_CONDITIONS" => "$(inherited) LINPHONE_ENABLED"
  }
end
