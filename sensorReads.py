import paho.mqtt.client as tempReads

import time
import board
import adafruit_dht
import RPi.GPIO as GPIO
from gpiozero import MotionSensor
import pygame.camera
from datetime import datetime
import subprocess

channelRain = 18

GPIO.setmode(GPIO.BCM)
# Receive input signals through the pin.
GPIO.setup(channelRain, GPIO.IN)

pir = MotionSensor(17)
sensor = adafruit_dht.DHT22(board.D4, use_pulseio=False)

width =640
height =480
i=0
picTaken = False

broker = "localhost"

port=1883

#set the topic – choose your own path
topic = "A00243808/group"

#create a client using the mqtt client imported at the start
#” anyclientid” identifies the client that uses this code e.g.
client1 = tempReads.Client("456")
# connect the client to the broker
client1.connect(broker, port)

pygame.init()
pygame.camera.init()
camera=pygame.camera.Camera("/dev/video0",(width,height))
camera.start()
windowSurfaceObj=pygame.display.set_mode((width,height),1,16)
pygame.display.set_caption("Camera")

#update camera 
def update_camera():
    image = camera.get_image()
    catSurfaceObj = image
    windowSurfaceObj.blit(catSurfaceObj,(0,0))
    pygame.display.update()

#take photo when motion is detected
def take_pic():
    
    update_camera() 
    global i
    i = i + 1
    pygame.image.save(windowSurfaceObj,'/home/pi/Documents/Group/pic%02d.jpg'%(i))
    print('Motion detected! A picture has been taken')
    subprocess.call(['sh', './sync.sh'])
    messagePic = "Motion Detected. Picture Uploaded\n"
    #send app message that image was taken and uploaded
    client1.publish(topic,messagePic)   


while True:
    try:
        
        update_camera()
        #Check water sensor for water       
        if GPIO.input(channelRain):
            water = "No Water Detected!!"
        else:
            # 'Water' = 0/False (microcontroller light is on).
            water = "Water Detected"
   
        #take a temperature and humidity reading using the dht22 sensor
        temp = sensor.temperature
        hum = sensor.humidity
        
        #get current time and date
        timeDate = datetime.now()
        
        pir.when_motion = take_pic
        
    
        #publish the temperature reading to a topic e.g. “room1/sensehat/temp”
        message = "Date & Time: " + str(timeDate) + "\nTemperature: " +str(temp) + "\nHumidity: " + str(hum) + "\n" + str(water) +"\n"
        client1.publish(topic,message)
 
       
       #print the temperature and humidity to the screen
        print(timeDate)
        print (
                "Temp:{:.1f} C    Humidity: {}% ".format (temp, hum) 
            )
        print(water + "\n")
        
    except RuntimeError as error:
        # Errors happen fairly often, DHT's are hard to read, just keep going
        print(error.args[0])
        time.sleep(2.0)
        continue
    except Exception as error:
        dhtDevice.exit()
        raise error
   #wait 5 second before repeating
    time.sleep(5.0)