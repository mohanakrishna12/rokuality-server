# Rokuality Server - End to End Automation for Roku!

The Rokuality Server allows you to distribute Roku end to end tests across multiple Roku devices on your network. (Playstation and XBox coming soon!) The server acts a lightweight web proxy, capturing your Roku test traffic and distributing the tests to the devices provided in your capabilities. Finally - an open source end to end test automation platform for Roku! No proprietary closed 3rd party software, and no complicated "magic box" capture hardware required! Just put your Roku in dev mode, start the server, and pick a binding to start writing tests in!

### Getting started: Enabling Developer Mode on your Roku
[Enabling developer mode](https://blog.roku.com/developer/developer-setup-guide) on your Roku device is very straight forward. Keep track of your device username and password as created during the basic walkthrough as you'll need them to pass to your DeviceCapabilities at driver startup. Once you've enabled developer mode on your device you should be able to hit the device console page at http://yourrokudeviceip

### Getting started: Server Requirements
The Rokuality Server is a Java Jetty servlet container application that requires at least Java 8 to run, and that you have Java available on your PATH. Typically the easiest way to do that is to use the relevant package manager for the Operating System you are running. For example, for MAC users it's easy using [brew](https://brew.sh/):
```xml
    brew cask install java
```
or for Windows users using [scoop](https://scoop.sh/)
```xml
    scoop bucket add java
	scoop install openjdk10
```

If you have it setup correctly you should be able to run the following command and get a relevant version back:
```xml
    java -version
```
should return something like:

```xml
    java version "1.8.0_221"
    Java(TM) SE Runtime Environment (build 1.8.0_221-b11)
    Java HotSpot(TM) 64-Bit Server VM (build 25.221-b11, mixed mode)
```

### Getting started: Server OCR Requirements
The server uses Tesseract as the default OCR module, so tesseract must be installed and available on your system's PATH. Tesseract is most easily installed on MAC via:
```xml
    brew install tesseract
```
or on Windows via:
```xml
    scoop install tesseract
```

### Getting started: OS Requirements
The Rokuality Server should work on MacOS X 10.11 or higher, Windows 10 or Windows Server (2012 or higher). It has not been tested yet for linux but 'should' work.

### Getting started: Running the Server via Maven
An easy way to start the server is to clone the repository, cd into the main directory, and then compile on the fly. Note that with this approach you will always be running the latest master build, but if you prefer to run a static release version, see [the jar](#getting-started-running-the-server-jar) instructions below. If you don't have maven it can be installed via your relevant Operating System package manager. i.e for MAC:
```xml
    brew install maven
```
Or for Windows if you're using scoop:
```xml
    scoop install maven
```
And once you have Maven installed, start the server by:

```xml
    cd /path/to/where/you/cloned/rokuality-server/
    mvn clean compile -e exec:java
```
By default this will start the server listening at port 7777. Optionally you can provide an alternate port for the server via:
```xml
    mvn clean compile -e exec:java -Dport=yourport
```

### Getting started: Running the Server JAR
Optionally you can go to the [releases page](https://github.com/rokuality/rokuality-server/releases) and download the latest standalone Server JAR. Once downloaded you can run via:
```xml
    java -Dport=youport -jar rokuality-server.jar
```

Upon a successful server launch you should see a pop indicating that the server is ready and listening. If it's the first time you are launching the server it will take about a minute while it downloads and installs the necessary tesseract trained data files in the background.

### Server command options:
| Command  | Description | Notes |
| ------------- | ------------- | ------------- |
| port | Starts the server on a port provided by the user.  | If not provided the server defaults to listen on port 7777 |
| commandtimeout | Specifies a session cleanup time in seconds for any orphaned sessions i.e. any sessions that were started but haven't received any commands for a specified duration.  | If not provided defaults to 60 seconds. |

### Next Steps
Pick a language binding to begin writing tests:

[Java](https://github.com/rokuality/rokuality-java)

JS (coming soon - contact us to get involved)

C# (coming soon - contact us to get involved)

Python (coming soon - contact us to get involved)


### Notes
1. This a new project but the goal is to add additional features for other streaming devices (Playstation, XBox, and SetTop based devices) in the near future. There will be a roadmap soon highlighting new features and goals.
2. Additionally if you're interested in helping and have experience in any of the above mentioned languages - please contact us to get involved!
3. The server has been tested for Windows and MAC but should work for various flavors of linux as well. But please log [issues](https://github.com/rokuality/rokuality-server/issues) if you find them and they'll be worked on as quickly as possible.