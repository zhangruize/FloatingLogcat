# FloatingLogcat
Android. A floating overlay view display logcat info, support tag filter edit.

# ScreenShot
![ScreenShot](https://github.com/zhangruize/FloatingLogcat/blob/master/art/e416b51e559130af301956e77d7faef.png?raw=true)
      
# OsVersionRequire
Currently, it only support api>=23.

# Gradle
Step 1. Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

Step 2. Add the dependency
```
	dependencies {
	        implementation 'com.github.zhangruize:FloatingLogcat:Tag'
	}
```

Step 3. Show the floating logcat
```
new FloatingConsoleView(getApplicationContext());
```

# Usage
Longpress logcat view to switch setting page or logcat page. In setting page, you can set filter tag.
