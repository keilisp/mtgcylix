# mtgcylix
Babashka script for converting Magic: The Gathering collection managers csv formats such as [Dragon Shield](https://mtg.dragonshield.com/) and [MTGGoldfish](https://www.mtggoldfish.com/).

It's pretty simple and doesn't take different card versions into account (unless 2 csv formats are compatible to do this by design.)

Design is polymorphic, so everyone can add support for conversions by just specifying csv column mappings between 2 desired formats. Also possible to fine-tune specific column-to-column conversions by providing lambdas to do so.

Information on (most) formats differences: https://www.mtggoldfish.com/help/import_formats
## Usage
```bash
./cylix.clj -s format -r format -f path -d path
```
or using `babashka`

```bash
bb cylix.clj -s format -r format -f path -d path
```

### --help :
```
  -s, --source format                   Format of the source file you have already generated.
  -r, --result format                   Format you want convert to.
  -f, --file path                       Path to source file.
  -d, --dest path      ./converted.csv  Path to the resulting converted file.
  -h, --help

Supported formats:
{"https://mtg.dragonshield.com" "DragonShield",
 "https://www.mtggoldfish.com" "Goldfish"}
```

## Example
Source file in DragonShield Collection Manager format `cat dragonshield.csv`:
```
"sep=,"
Folder Name,Quantity,Trade Quantity,Card Name,Set Code,Set Name,Card Number,Condition,Printing,Language,Price Bought,Date Bought,LOW,MID,MARKET
cylix,1,0,"Boseiju, Who Endures",NEO,Kamigawa: Neon Dynasty,266,NearMint,Normal,English,36.34,2023-04-03,32.63,38.55,36.34
cylix,1,0,Thoughtseize,THS,Theros,107,NearMint,Normal,English,13.04,2023-04-03,11.00,15.45,13.04

```

Convert with
```bash
./cylix.clj -s DragonShield -r Goldfish -f ./dragonshield.csv -d ./goldfish.csv

```

Result, which is compatible with Goldfish Collection import:
`cat goldfish.csv`
```
Quantity,Card,Set ID,Set Name,Variation,Foil
1,"Boseiju, Who Endures",NEO,Kamigawa: Neon Dynasty,,REGULAR
1,Thoughtseize,THS,Theros,,REGULAR

```
