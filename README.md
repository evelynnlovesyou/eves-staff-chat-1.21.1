**Commands**
> /staffchat \<message\>
- Allows staff to chat privately without sneaky players viewing the messages. Permission nodes: `evesstaffchat.<Chat_Name>.send`, `evesstaffchat.<Chat_Name>.send` and `evesstaffchat.<Chat_Name>.receive` (defaults to OP if LuckPerms is not found)

> /staffchattoggle or /staffchat (no args)
- Toggles staff-chat mode — while enabled, everything you type goes to staff chat. Needs to be toggled off again to send regular messages. Permission node: `evesstaffchat.staffchat.toggle`

> /evesstaffchat reload
- Reloads all configs. Permission node: `evesstaffchat.reload`

**Configuration**
> Multiple chats can be defined in `chats.json`, each with their own command, permission base, and message format. A default `staffchat` entry is created automatically.

> `lang.json` lets you customise every message the mod sends to players.

> `config.json` has an `use_action_bar` option to send toggle/feedback messages on the action bar instead of chat.
