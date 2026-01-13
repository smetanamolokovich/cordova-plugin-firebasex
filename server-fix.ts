// OPRAVA pro sendPushNotification
// Nahraďte tuto část kódu:

notificationPayload.android = {
    priority: 'high',
    notification: {
        title: title,
        body: message,
        sound: 'default',
        click_action: 'FLUTTER_NOTIFICATION_CLICK',
        ...(image && { image: image }),
    },
}

// Kompletní opravený payload by měl vypadat takto:

const notificationPayload: any = {
    data: {
        notification_foreground: 'true',
        notification_title: title,
        notification_body: message,
        title: title,
        body: message,
        ...additionalData,
    },
}

if (url) {
    notificationPayload.data.route = url
}

if (actions && actions.length > 0) {
    notificationPayload.data.actions = JSON.stringify(actions)
}

// DŮLEŽITÉ: Pro Android přidáme notification objekt
notificationPayload.android = {
    priority: 'high',
    notification: {
        title: title,
        body: message,
        sound: 'default',
        click_action: 'FLUTTER_NOTIFICATION_CLICK',
        ...(image && { image: image }),
    },
}

// iOS zůstává stejné
notificationPayload.apns = {
    payload: {
        aps: {
            alert: {
                title: title,
                body: message,
            },
            sound: 'default',
            'content-available': 1,
            'mutable-content': 1,
        },
    },
}

if (iosCategory) {
    notificationPayload.apns.payload.aps.category = iosCategory
}

if (additionalData) {
    Object.keys(additionalData).forEach((key) => {
        notificationPayload.apns.payload[key] = additionalData[key]
    })
}

if (url) {
    notificationPayload.apns.payload.route = url
}

if (actions && actions.length > 0) {
    notificationPayload.apns.payload.actions = JSON.stringify(actions)
}

if (image) {
    notificationPayload.apns.fcm_options = {
        image,
    }
}

/* 
POZNÁMKA:
- S notification objektem v android sekci se notifikace zobrazí automaticky i když je app zavřená
- Vaše FirebasePluginMessagingService dostane data i notification
- Action buttons budou fungovat protože data obsahují actions
- Funguje i na Lenovo a jiných zařízeních s agresivní optimalizací
*/
