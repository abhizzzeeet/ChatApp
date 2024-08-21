Its a Chat Application which allows logged in users to send and recive messages . App uses Realtime Database of Firebase to store messages and user related data and also uses Firebase Cloud Messaging for sending notifications in real time.
App is mostly made in Kotlin , and uses NodeJs to build a serverless architecture in Google Cloud Functions. Firebase Cloud Messaging used for sending notification to reciver.
                            
App provides following features:                      
  -> Allow SignIn and SignUp using Firebase email and password authentication                     
  -> Loads personal contacts from local device of user and shows those users for chatting if searched                      
  -> Sends notification to receiver on sending message if the chat of sender is not opened in receiver's device                                       
  -> Use serverless architecture by deploying firebase functions to Google Cloud Functions     
                     
Text Stack: Kotlin, NodeJs .                              
                  
I have explained my project throudh a video demonstration below.     
You can SignUp a user and then login. When you try to search a user by typing a text or mobile number on search box it will show three lists of matching users :      
1. Chats : List of matching users with whom you have chatted with
2. Other Contacts : List of matching users whom you have not chat with but are a user of app
3. Invite to app: List of matching users who are in your local device contacts but are not user of app               
                 
Steps to run app :                       
-> Run the frontend folder in android studio by clicking Run button           
-> For deploying a new function in firebase run the command "firebase deploy --only functions" in root directory , although I have already deployed a function in firebase for serverless architecture , so its not necessary


https://github.com/user-attachments/assets/37d5ed2c-6f30-4e1b-945f-51eab1c69794

