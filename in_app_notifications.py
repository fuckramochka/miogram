"""
id: in_app_notifications
name: In-App Notifications
version: 1.0.0
author: @Miogram
description: Відображає спливаючі сповіщення (банери) всередині додатку при отриманні нових повідомлень, коли ви знаходитесь в іншому чаті або меню.
icon: msg_notifications
"""

from base_plugin import BasePlugin, MethodHook
from java import jclass
import json

class InAppNotificationsPlugin(BasePlugin):

    def on_plugin_load(self):
        self.log("In-App Notifications plugin loaded successfully.")
        try:
            # Hook NotificationsController or NotificationCenter to catch incoming messages
            AndroidUtilities = jclass("org.telegram.messenger.AndroidUtilities")
            BulletinFactory = jclass("org.telegram.ui.Components.BulletinFactory")
            LaunchActivity = jclass("org.telegram.ui.LaunchActivity")
            R = jclass("org.telegram.messenger.R")

            class MessageNotificationHook(MethodHook):
                def __init__(self, plugin):
                    super().__init__()
                    self.plugin = plugin

                def after_hooked_method(self, param):
                    try:
                        # param.args: id, account, args...
                        notif_id = int(param.args[0])
                        # NotificationCenter.didReceiveNewMessages = 2
                        if notif_id == 2 or notif_id == 1:
                            act = LaunchActivity.instance
                            if act is not None and not act.isFinishing():
                                fragment = act.getSafeLastFragment()
                                if fragment is not None:
                                    def show_bulletin():
                                        try:
                                            b = BulletinFactory.of(fragment).createSimpleBulletin(
                                                R.drawable.msg_notifications,
                                                "Нове повідомлення"
                                            )
                                            b.show()
                                        except Exception:
                                            pass
                                    AndroidUtilities.runOnUIThread(show_bulletin)
                    except Exception as e:
                        pass

            self.hook_method(
                "org.telegram.messenger.NotificationCenter",
                "postNotificationNameInternal",
                MessageNotificationHook(self)
            )
            self.log("Message notification hook registered successfully.")
        except Exception as e:
            self.log(f"Failed to hook NotificationCenter: {e}")

    def on_plugin_unload(self):
        self.log("In-App Notifications plugin unloaded.")
