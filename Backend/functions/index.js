const functions = require("firebase-functions");
const admin = require("firebase-admin");
const cors = require("cors")({origin: true});

// Initialize Firebase Admin SDK
// if (!admin.apps.length) {
admin.initializeApp();
// }

exports.sendNotification = functions.https.onRequest((req, res) => {
  cors(req, res, async () => {
    const {receiverId, messageText} = req.body;

    if (!receiverId || !messageText) {
      return res.status(400).send("Missing receiverId or messageText");
    }

    try {
      // Fetch the receiver's device token from the database
      const userSnapshot = await admin.database().ref(`users/${receiverId}`).once("value");
      const user = userSnapshot.val();

      if (!user || !user.fcmToken) {
        return res.status(404).send("Receiver not found or FCM token missing");
      }

      const token = user.fcmToken;

      const message = {
        notification: {
          title: "New Message",
          body: messageText,
        },
        token: token, // Send to receiver's device token
      };

      const response = await admin.messaging().send(message);
      res.status(200).send(`Notification sent successfully: ${response}`);
    } catch (error) {
      res.status(500).send(`Error sending notification: ${error.message}`);
    }
  });
});
