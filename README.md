# Rokuality Server - End to End Automation for Roku and XBox!

The Rokuality Server allows you to distribute Roku and XBox end to end tests across multiple devices on your network. (Playstation, SetTop, and others coming soon!) The server acts a lightweight web proxy, capturing your device test traffic and distributing the tests to the devices provided in your capabilities. The project goal is to provide a no cost/low cost open source solution for various video streaming platforms that otherwise don't offer an easily automatable solution! Once you have the server setup with all requirements, [pick a language](#next-steps-choose-a-test-language) to write tests in.

### Getting started: Server Requirements
The Rokuality Server is a Java Jetty servlet container application that requires at least Java 8 to run, and that you have Java available on your PATH. Typically the easiest way to do that is to use the relevant package manager for the Operating System you are running. For example, for MAC users it's easy using [brew](https://brew.sh/) via `brew cask install java` or for Windows users using [scoop](https://scoop.sh/) via 
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
The server uses Tesseract as the default OCR module, so tesseract must be installed and available on your system's PATH. Tesseract is most easily installed on MAC via `brew install tesseract` or on Windows via `scoop install tesseract`

### Getting started: OS Requirements
The Rokuality Server should work on MacOS X 10.11 or higher, Windows 10 or Windows Server (2012 or higher). It has not been tested yet for linux but 'should' work.

### Getting started: Running the Server via Maven
An easy way to start the server is to clone the repository, cd into the main directory, and then compile on the fly. Note that with this approach you will always be running the latest master build, but if you prefer to run a static release version, see [the jar](#getting-started-running-the-server-jar) instructions below. If you don't have maven it can be installed via your relevant Operating System package manager. i.e for MAC via `brew install maven` Or for Windows if you're using scoop `scoop install maven`

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

### Getting started: Roku
Roku testing requires that you have developer mode enabled on your device. [Enabling developer mode](https://blog.roku.com/developer/developer-setup-guide) on your Roku device is very straight forward. Keep track of your device username and password as created during the basic walkthrough as you'll need them to pass to your DeviceCapabilities at driver startup. Once you've enabled developer mode on your device you should be able to hit the device console page at http://yourrokudeviceip - Once that's done you are all set for automation!

### Getting started: XBox
Automated testing on XBox requires the following:
1. Google Chrome browser installed on the machine running the Rokuality server. The server uses headless chrome to handle various tasks like installing/launching/uninstalling the XBox appxbundles. You won't physically see chrome launch as it will run in headless mode.
2. Your XBox must be in dev kit mode. [Enabling developer mode](https://docs.microsoft.com/en-us/windows/uwp/xbox-apps/devkit-activation) on your XBox is straight forward but it does require a 19$ Microsoft developer account which will allow you to automate 3 boxes from a single dev account.
3. You must have the [XBox dev console](https://docs.microsoft.com/en-us/windows/uwp/xbox-apps/device-portal-xbox) available for remote access with NO username/password set. If properly setup you should be able to access your dev console remotely at `https://yourxboxip:11443`
4. You must have nodejs installed on the machine running the server. Easily done on MAC via `brew install node` or on Windows via `scoop install nodejs`.
5. You must have a [Logitech Harmony Hub](https://www.logitech.com/en-us/product/harmony-hub?crid=60) with your XBox setup as a device and XMPP enabled on the hub. See the sections [why harmony](#why-harmony) and [configuring your harmony](#configuring-your-harmony) for details.

### Server command options:
| Command  | Description | Notes |
| ------------- | ------------- | ------------- |
| port | Starts the server on a port provided by the user.  | OPTIONAL - Defaults to listen on port 7777 |
| commandtimeout | Specifies a session cleanup time in seconds for any orphaned sessions i.e. any sessions that were started but haven't received any commands for a specified duration.  | OPTIONAL - Defaults to 60 seconds. |
| threads | The maximum number of allowed threads the server can handle. | OPTIONAL - Defaults to 10

### Next Steps: Choose a test language:
Pick a language binding to begin writing tests:

[Java](https://github.com/rokuality/rokuality-java)

[Python](https://github.com/rokuality/rokuality-python)

JS (coming soon - contact us to get involved)

C# (coming soon - contact us to get involved)



### Why Harmony
Some of our device automation setups require a [logitech harmony](https://www.logitech.com/en-us/product/harmony-hub?crid=60) to drive the user input. The hub is a low cost (60$) device that gives us both IR and Bluetooth capability in an easily available, wireless solution. So we don't need to use an IR blaster or a cabled base solution. And we can scale across additional devices easily. Thanks to the awesome [harmony cli](https://github.com/sushilks/harmonyHubCLI) project, we are able to incorporate a simple cli solution inside of the Rokuality server to remotely drive the harmony and control the devices. Other similar solutions on the market ship you a "magic box" device that requires you to plug in your device under test - which is proprietary and limits the ability to scale with more devices and across multiple developers. Using the harmony approach is cheap, effective, and scalable.

### Configuring Your Harmony
To setup your Harmony hub and prepare it for automating your devices under test:
1. Download the Harmony mobile app for iOS or Android.
2. During setup it will walk you through adding your device under test. Be sure to keep track of what you name your device when you pair it with your Harmony as that will be used later during your tests as your "DeviceName" capability. This will allow the Rokuality server to communicate with your device from your test code and drive it like a real user.
3. Enable XMPP on the hub. In the harmony app go to Settings>>Harmony Setup>>Add/Edit Devices & Activities>>Hub>>Enable XMPP

### Notes
1. This a new project but the goal is to add additional features for other streaming devices (Playstation, XBox, and SetTop based devices) in the near future. There will be a roadmap soon highlighting new features and goals.
2. Additionally if you're interested in helping and have experience in any of the above mentioned languages - please contact us to get involved!
3. The server has been tested for Windows and MAC but should work for various flavors of linux as well. But please log [issues](https://github.com/rokuality/rokuality-server/issues) if you find them and they'll be worked on as quickly as possible.