# DukesMart
Chest shop plugin without Vault implementation; item-based

## Features
1. Easy to use and no cryptic IDs needed unlike other plugins
2. Sell customized items, such as written books, enchanted items, banners, and more!
3. Detailed displays showing every detail about an item
4. Notification system to alert you got money
5. Intuitive command system for ease of use

----

## Setup
### System Requirements
1. MariaDB, MySQL, or other SQL server
2. A SQL user who has CREATE TABLE, SELECT, UPDATE, INSERT, and DELETE priviledges

### Installation
1. Place the DukesMart.jar file in your server's plugin folder
2. Start your server; As your server loads DukesMart, the plugin will generate a config.yml file
3. Replace the values in config.yml with your database/user information
4. Enjoy!

## How to use DukesMart
**DukesMart** is made with players of all ages in mind. As such, it is straight-forward compared to other chest shop plugins.

### Your ledger
Every player will be given a **ledger**. The ledger stores income from all shops you own. Your ledger is safe from theft, but it is not your personal bank. Any income in your ledger will expire if left unattended for **forty-five days**. Make sure you withdraw *all* of your ledger income before then!

### Creating a shop
You will need the following items: **chest**, **sign**, and of course an **item** to sell.

1. Place a chest on the ground. Double chests work as well.
2. Place the sign either on or above the chest.
3. On the **first** line of the sign, type in "[buy]" (case-insensitive).
4. On the **third** line of the sign, type in the format "*x* for $*y*", where *x* is the quantity and *y* is the price.
    1. For example, "5 for $8" means five of the item for $8
5. If the sign is formatted correctly, the first line will turn purple, a white **?** appears on the second line, and your username will appear on the fourth line in dark blue
6. Next, place the item you want to sell in your hand.
7. With item in hand, right-click on your newly created sign.
8. The white **?** on the second line will now read the name of your item. If your item has a custom name, such as a written book, it will display that instead.
9. Place all items to sell in the chest below.
10. Done!

**Warning!**: DukesMart does **not** lock your shop chest, nor protect the sign itself. Make sure to place everything in a safe location and use appropriate lock signs, such as Lockette.

### Buying from a shop
1. Find a shop to buy from.
2. Right-click on the sign.
3. You will see the message "Sign selected" in chat, and a scoreboard display to the right of your screen.
4. Carefully review the item, attributes, pricing, etc. listed in the display.
5. Right-click the sign again to complete your purchase.
    1. Make sure you have enough money (gold) in your inventory as well as free space for the item!
6. Continue to right-click the sign to make additional purchases.

### Commands
**DukesMart** features a handful of useful commands.

#### All players
1. /shop - Base command. Run this in-game to see a full list of all commands.
2. /shop balance - View your ledger balance.
3. /shop top - View top ten highest-earning players on the server.
4. /shop withdraw - Withdraws all money from your ledger.
5. /shop withdraw {amount} - Withdraws a specified amount of money from your ledger.

#### Admin commands
To use these commands, you must have the dukesmart.shop.admin permission.

1. /shop balance {player name} - View the specified player's ledger balance.
2. /shop history {player name} - Views ten most recent transactions made by a given player. Player can be online or offline.
3. /shop view recent - Views ten most recent transactions for a given shop. You must select a shop in-game first.

Check this document for additional commands in future releases.

## Limitations
1. Shulker boxes containing items may not be sold. Empty shulker boxes, however, are allowed to be sold.
2. For technical reasons, dyed leather armor will not display the color name
3. While you may set your prices at any value, the maximum possible price is $2304 (max stacks in every inventory slot)
4. Currently, you may not set the quantity of an item greater than that item's maximum stack (Eg. a dirt block's maximum stack is 64.) DukesMart will correct any invalid quantity automatically.