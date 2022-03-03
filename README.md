# TCP-UDP-communication
Server-Client info exchange using TCP and UDP protocols.

The purpose of the code is to exhibit some advanced data receiving and decoding techniques dealing with echo,
image and sound packets. In order to be able to receive and send such information to a server, it is essential 
that you modify the appropriate settings of your router(port forwarding enabled etc). This specific code cannot 
be ran manually by any users because it was created only to communicate with a specific university server in Greece.
So you may just get ideas and implement them on your own code to exchange info with any server (maybe your own 
virtual one). Below follows a quick function quide.

~ ImageRequest() ~

It requests with an image-specific code and the server responds with UDP packets containing the pixels in binary form.
Calling the "SaveAndDisplay()" function after the image list has been formed, we are able to 1)save the image in a binary 
file with a "JPEG" extension, and 2) display the image in a frame on our screen.

~ EchoRequest() ~

This function accepts an echo as a String which is a message displaying the local time and day.

~ RemoteControl() ~

The "RemoteControl()" function gives the user the opportunity to track in detail some parameters of a BRDM vehicle.
As the vehicle moves we track the speed, engine temperature, intake air etc, by decoding the incoming messages
using the BRDM decoding catalogues. With a "for" loop we send alternating request codes(one for each parameter)
and after the receival we do the necessary decoding determined by the "switch".

~ Telemetry() ~

Remote flying of a mini-drone platform is available through the university's server, which accepts the engine rpm of 
the minicopter as argument of the "Telemetry()" function. The drone can be watched in live time from the web while we
manually impose its height.

~ Measurements() ~

This is about measuring the throughput(BPS) of the server, as well as the RTO, SRTT parameters using as RTT the 
response time for as long as the user desires.

~ AudioRequest(_AQ)_DPCM() ~

By far the most fascinating part of the code is the binary decoding of a DPCM/AQDPCM-format UDP packets. The byte
array containing the sound bytes is saved as a "WAVE" file, using an appripriately synchronized thread which 
records the audio as it is played after the download.

