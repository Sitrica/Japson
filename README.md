# Japson
### A Java Protocol using Gson with Heartbeat

Report all issues in our issues tab.

### Maven
Maven requires setting up profiles and defining the token else where https://help.github.com/en/github/managing-packages-with-github-packages/configuring-apache-maven-for-use-with-github-packages

### Gradle
Latest version can be found at https://github.com/Sitrica/Japson/packages

In your `build.gradle` add: 
```groovy
repositories {
	maven {
		url 'https://maven.pkg.github.com/Sitrica/Japson/'
		credentials {
			username = "<INSERT USERNAME>"
			password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_PACKAGES_KEY")
		}
	}
}

dependencies {
	compile (group: 'com.sitrica', name: 'japson', version: 'INSERT VERSION')
}
```
Getting a Github token:

1.) Go into your account settings on Github and create a personal token with the read:packages scope checked.

2.) Generate that key, and now go add a System Environment Variable named GITHUB_PACKAGES_KEY
or set the gradle property "gpr.key" to your key.

3.) Restart system or if using Chocolatey type `refreshenv`

Note: you can just directly put your token as the password, but we highly discourage that.

### Usage

Client side:
```java
try {
	japson = new JapsonClient("example.com", 1337);
	japson.sendPacket(new Packet((byte)0x01) {
		@Override
		public String toJson(Gson gson) {
			object.addProperty("property", "example");
			return gson.toJson(object);
		}
	});
} catch (UnknownHostException e) {
	e.printStackTrace();
}
```

Server:
```java
try {
	japson = new JapsonServer(1337);
	japson.registerHandlers(new Handler((byte)0x01) {
		@Override
		public String handle(InetAddress address, int port, JsonObject object) {
			String property = object.get("property").getAsString();
			System.out.println(property);
			return null; // Return a gson.toJson value from a JsonElement for a ReturnablePacket.
		}
	});
} catch (UnknownHostException | SocketException e) {
	e.printStackTrace();
	return;
}
```
