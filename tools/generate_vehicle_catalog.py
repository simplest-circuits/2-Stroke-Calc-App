#!/usr/bin/env python3
"""Generate vehicle_catalog.json from curated technical data."""
import json
from pathlib import Path

OUT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "assets" / "vehicle_catalog.json"

def e(id_, brand, model, variant, frame, cat, yf, yt, vtype, cycle="TWO_STROKE", **kw):
    entry = {
        "id": id_, "brand": brand, "model": model, "variant": variant,
        "frameCode": frame, "category": cat, "yearFrom": yf, "yearTo": yt,
        "vehicleType": vtype, "cycleType": cycle,
        "engine": kw.get("engine", {}),
        "carbIgnition": kw.get("carb", {}),
        "drivetrain": kw.get("drive", {}),
        "chassis": kw.get("chassis", {}),
        "electrical": kw.get("elec", {}),
    }
    return entry

ENTRIES = []

# ── VESPA KLASSIK ──────────────────────────────────────────────────────────────
VESPA = [
 e("vespa_px_125_mk1","Vespa","PX 125","","VNX1T","Klassik Vespa",1978,1981,"ROLLER",
   engine={"displacementCc":"123,4","boreMm":"52,5","strokeMm":"57","compressionRatio":"8,2:1","coolingType":"Luft","intakeSystem":"Membran","portTimingNotes":"3 Überströmer ab 1978"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"98","pilotJet":"45","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"21","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingPrimary":"21/68","gearingSecondary":"12/57 · 13/42 · 17/38 · 21/35"},
   chassis={"frontTire":"3.50-10","rearTire":"3.50-10","tirePressureFront":"1,7","tirePressureRear":"1,9","frontBrake":"Trommel 150 mm","rearBrake":"Trommel 150 mm"},
   elec={"battery":"12V","alternator":"6V Lichtmaschine","electricalNotes":"Später 12V Umbau üblich"}),

 e("vespa_px_125_e","Vespa","PX 125 E","Elestart","VNX2T","Klassik Vespa",1981,1997,"ROLLER",
   engine={"displacementCc":"123,4","boreMm":"52,5","strokeMm":"57","compressionRatio":"8,2:1","coolingType":"Luft","intakeSystem":"Membran","portTimingNotes":"3 Überströmer"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"99","pilotJet":"45","needle":"BE5","fuelMixRatio":"automatisch","sparkPlug":"NGK B6HS","ignitionTimingDeg":"18","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingPrimary":"21/68","gearingSecondary":"12/58 · 13/42 · 17/38 · 21/36"},
   chassis={"frontTire":"3.50-10","rearTire":"3.50-10","tirePressureFront":"1,7","tirePressureRear":"1,9","frontBrake":"Trommel 150 mm","rearBrake":"Trommel 150 mm"},
   elec={"battery":"12V","alternator":"12V","electricalNotes":"E-Starter ab Werk"}),

 e("vespa_px_125_e_98","Vespa","PX 125 E","Millennium '98-'05","VNX2T","Klassik Vespa",1998,2005,"ROLLER",
   engine={"displacementCc":"123,4","boreMm":"52,5","strokeMm":"57","compressionRatio":"8,5:1","coolingType":"Luft","intakeSystem":"Membran","portTimingNotes":"3 Überströmer"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"99","pilotJet":"45","needle":"BE5","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionTimingDeg":"18","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung 4 Scheiben","gearingPrimary":"21/68"},
   chassis={"frontTire":"3.50-10","rearTire":"3.50-10","frontBrake":"Trommel 150 mm","rearBrake":"Trommel 150 mm"},
   elec={"battery":"12V","alternator":"12V"}),

 e("vespa_px_80","Vespa","PX 80","","V8X1T","Klassik Vespa",1981,1999,"ROLLER",
   engine={"displacementCc":"78,9","boreMm":"47","strokeMm":"45,5","compressionRatio":"8,2:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SI 16/16","mainJet":"82","pilotJet":"38","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingPrimary":"20/68","gearingSecondary":"10/59 · 14/55 · 19/50 · 23/47"},
   chassis={"frontTire":"3.50-10","rearTire":"3.50-10","frontBrake":"Trommel","rearBrake":"Trommel"}),

 e("vespa_px_150","Vespa","PX 150","","VLX1T","Klassik Vespa",1978,1984,"ROLLER",
   engine={"displacementCc":"149","boreMm":"57,8","strokeMm":"57","compressionRatio":"8,2:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"102","pilotJet":"45","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"21","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingPrimary":"21/68"}),

 e("vespa_px_200","Vespa","PX 200","","VSX1T","Klassik Vespa",1977,1984,"ROLLER",
   engine={"displacementCc":"198","boreMm":"66,5","strokeMm":"57","compressionRatio":"9,8:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SI 24/24E","mainJet":"116","pilotJet":"50","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingPrimary":"23/64"}),

 e("vespa_px_200_e","Vespa","PX 200 E","Elestart","VSX2T","Klassik Vespa",1984,1997,"ROLLER",
   engine={"displacementCc":"198","boreMm":"66,5","strokeMm":"57","compressionRatio":"9,8:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SI 24/24E","mainJet":"118","pilotJet":"50","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("vespa_t5","Vespa","T5","125 Elestart","VNX5T","Klassik Vespa",1985,1991,"ROLLER",
   engine={"displacementCc":"123","boreMm":"55","strokeMm":"52","compressionRatio":"11,3:1","coolingType":"Luft","intakeSystem":"Membran","portTimingNotes":"5 Überströmer"},
   carb={"carbType":"Dell'Orto SI 24/24G","mainJet":"110","pilotJet":"50","needle":"BE4","fuelMixRatio":"automatisch","sparkPlug":"NGK B6ES","ignitionTimingDeg":"23","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("vespa_cosa_125","Vespa","Cosa","125","VNR2T","Klassik Vespa",1988,1993,"ROLLER",
   engine={"displacementCc":"123,4","boreMm":"52,5","strokeMm":"57","compressionRatio":"8,5:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"99","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("vespa_pk_50","Vespa","PK 50","","V5X1T","Klassik Vespa",1983,1989,"ROLLER",
   engine={"displacementCc":"48","boreMm":"38,4","strokeMm":"43","compressionRatio":"8,5:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SHB 16/10","mainJet":"62","pilotJet":"38","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingSecondary":"4-Gang"},
   chassis={"frontTire":"3.00-10","rearTire":"3.00-10"}),

 e("vespa_pk_125","Vespa","PK 125","","VMX1T","Klassik Vespa",1983,1986,"ROLLER",
   engine={"displacementCc":"121,1","boreMm":"55","strokeMm":"51","compressionRatio":"9,5:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SHB 19/19","mainJet":"74","pilotJet":"42","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"17","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("vespa_pk_125_xl","Vespa","PK 125 XL","Elestart","VMX5T","Klassik Vespa",1986,1990,"ROLLER",
   engine={"displacementCc":"121,1","boreMm":"55","strokeMm":"51","compressionRatio":"9,5:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto SHB 19/19E","mainJet":"73","pilotJet":"45","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"}),

 e("vespa_primavera_125","Vespa","125 Primavera","","VMA2T","Klassik Vespa",1967,1982,"ROLLER",
   engine={"displacementCc":"121,1","boreMm":"55","strokeMm":"51","compressionRatio":"8,25:1","coolingType":"Luft","intakeSystem":"Membran","portTimingNotes":"2 Überströmer"},
   carb={"carbType":"Dell'Orto SHB 19/19","mainJet":"74","pilotJet":"45","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"25","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingPrimary":"24/61"}),

 e("vespa_et3","Vespa","ET3","","VMB1T","Klassik Vespa",1976,1990,"ROLLER",
   engine={"displacementCc":"121,1","boreMm":"55","strokeMm":"51","compressionRatio":"9,25:1","coolingType":"Luft","portTimingNotes":"3 Überströmer"},
   carb={"carbType":"Dell'Orto SHB 19/19","mainJet":"74","pilotJet":"45","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"24","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("vespa_125_gt","Vespa","125 GT","","VNL2T","Klassik Vespa",1966,1968,"ROLLER",
   engine={"displacementCc":"123,4","boreMm":"52,5","strokeMm":"57","compressionRatio":"7,8:1","coolingType":"Luft","portTimingNotes":"2 Überströmer"},
   carb={"carbType":"Dell'Orto SI 20/17","mainJet":"96","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"}),

 e("vespa_125_gtr","Vespa","125 GTR","","VNL2T","Klassik Vespa",1969,1978,"ROLLER",
   engine={"displacementCc":"123,4","boreMm":"52,5","strokeMm":"57","compressionRatio":"7,8:1","coolingType":"Luft","portTimingNotes":"2→3 Überströmer"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"98","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"16","ignitionSystem":"CDI"}),

 e("vespa_150_super","Vespa","150 Super","","VBC1T","Klassik Vespa",1965,1979,"ROLLER",
   engine={"displacementCc":"145,5","boreMm":"57","strokeMm":"57","compressionRatio":"6,5:1","coolingType":"Luft","portTimingNotes":"2 Überströmer"},
   carb={"carbType":"Dell'Orto SI 20/17","mainJet":"100","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"22","ignitionSystem":"Kontakt"},
   chassis={"frontTire":"3.50-10","rearTire":"3.50-10"}),

 e("vespa_150_sprint","Vespa","150 Sprint","","VLB1T","Klassik Vespa",1965,1976,"ROLLER",
   engine={"displacementCc":"145,5","boreMm":"57","strokeMm":"57","compressionRatio":"6,5:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto SI 20/15D","mainJet":"88","pilotJet":"42","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"},
   chassis={"frontTire":"3.50-8","rearTire":"3.50-8"}),

 e("vespa_150_vbb","Vespa","150","","VBB1T","Klassik Vespa",1960,1962,"ROLLER",
   engine={"displacementCc":"145,6","boreMm":"57","strokeMm":"57","compressionRatio":"6,8:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto SI 20/17","mainJet":"100","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"},
   chassis={"frontTire":"3.50-8","rearTire":"3.50-8"}),

 e("vespa_gs_160","Vespa","160 GS","","VSB1T","Klassik Vespa",1962,1964,"ROLLER",
   engine={"displacementCc":"158,5","boreMm":"58","strokeMm":"60","compressionRatio":"7,3:1","coolingType":"Luft","intakeSystem":"Kolbensteuerung"},
   carb={"carbType":"Dell'Orto SI 27/23","mainJet":"108","fuelMixRatio":"1:20","sparkPlug":"NGK B7HS","ignitionTimingDeg":"26","ignitionSystem":"Kontakt"}),

 e("vespa_180_ss","Vespa","180 Super Sport","","VSC1T","Klassik Vespa",1964,1968,"ROLLER",
   engine={"displacementCc":"181,1","boreMm":"62","strokeMm":"60","compressionRatio":"7,7:1","coolingType":"Luft","intakeSystem":"Kolbensteuerung"},
   carb={"carbType":"Dell'Orto SI 27/23","mainJet":"120","fuelMixRatio":"1:20","sparkPlug":"NGK B7HS","ignitionTimingDeg":"26","ignitionSystem":"Kontakt"}),

 e("vespa_180_rally","Vespa","180 Rally","","VSD1T","Klassik Vespa",1968,1973,"ROLLER",
   engine={"displacementCc":"180,7","boreMm":"63,5","strokeMm":"57","compressionRatio":"8:1","coolingType":"Luft","portTimingNotes":"3 Überströmer"},
   carb={"carbType":"Dell'Orto SI 20/20D","mainJet":"109","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionTimingDeg":"22","ignitionSystem":"Kontakt"}),

 e("vespa_200_rally","Vespa","200 Rally","","VSE1T","Klassik Vespa",1972,1979,"ROLLER",
   engine={"displacementCc":"198","boreMm":"66,5","strokeMm":"57","compressionRatio":"8,2:1","coolingType":"Luft","portTimingNotes":"3 Überströmer"},
   carb={"carbType":"Dell'Orto SI 24/24E","mainJet":"118","fuelMixRatio":"automatisch","sparkPlug":"NGK B7HS","ignitionTimingDeg":"24","ignitionSystem":"CDI"}),

 e("vespa_50_special","Vespa","50 Special","","V5A1T","Klassik Vespa",1969,1976,"ROLLER",
   engine={"displacementCc":"48,8","boreMm":"38","strokeMm":"43","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto SHB 16/16","mainJet":"62","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"},
   chassis={"frontTire":"2.75-9","rearTire":"2.75-9"}),

 e("vespa_90_ss","Vespa","90 SS","","V9SS1T","Klassik Vespa",1965,1971,"ROLLER",
   engine={"displacementCc":"88,5","boreMm":"47","strokeMm":"51","compressionRatio":"8,7:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto SHB 16/16","mainJet":"82","pilotJet":"38","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"Kontakt"}),
]
ENTRIES.extend(VESPA)

# ── VESPA MODERN ───────────────────────────────────────────────────────────────
ENTRIES.extend([
 e("vespa_gts_300","Vespa","GTS","300 HPE","ZAPM45300","Moderne Vespa",2019,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"278,3","boreMm":"75","strokeMm":"63","compressionRatio":"10,7:1","coolingType":"Flüssig","cylinderCount":"1"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik","clutchType":"Zentrifugalkupplung"},
   chassis={"frontTire":"120/70-12","rearTire":"130/70-12","frontBrake":"Scheibe","rearBrake":"Scheibe"}),

 e("vespa_gts_125","Vespa","GTS","125 iGet","ZAPM45100","Moderne Vespa",2016,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124,7","boreMm":"52,4","strokeMm":"57,8","compressionRatio":"10,5:1","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt SAE 5W-40","sparkPlug":"NGK LMAR8A-9","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik","clutchType":"Zentrifugalkupplung"},
   chassis={"frontTire":"110/70-11","rearTire":"120/70-11"}),

 e("vespa_lx_50","Vespa","LX","50 2T","ZAPC38100","Moderne Vespa",2005,2016,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"39,3","strokeMm":"41,1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik","clutchType":"Zentrifugalkupplung"}),

 e("vespa_lx_125","Vespa","LX","125 4T","ZAPM68300","Moderne Vespa",2005,2016,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,8","strokeMm":"56,4","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("vespa_primavera_50","Vespa","Primavera","50 2T","ZAPC53100","Moderne Vespa",2013,2026,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"39,3","strokeMm":"41,1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("vespa_primavera_125_4t","Vespa","Primavera","125 iGet 3V","ZAPMD11","Moderne Vespa",2016,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124,4","boreMm":"52","strokeMm":"58,6","compressionRatio":"10,5:1","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"SAE 5W-40","sparkPlug":"NGK LMAR8A-9","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik","clutchType":"Zentrifugalkupplung Ø 125 mm"},
   chassis={"frontTire":"110/70-11","rearTire":"120/70-11","frontBrake":"Scheibe","rearBrake":"Trommel"}),

 e("vespa_sprint_125","Vespa","Sprint","125 iGet 3V","ZAPMD11","Moderne Vespa",2016,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124,4","boreMm":"52,5","strokeMm":"58,6","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"SAE 5W-40","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),

 e("vespa_et2_50","Vespa","ET2","50","ZAPC16000","Moderne Vespa",1996,2005,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"39,3","strokeMm":"41,1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("vespa_et4_125","Vespa","ET4","125 4T","ZAPM19000","Moderne Vespa",1996,2005,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,8","strokeMm":"56,4","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),
])

# ── LAMBRETTA ──────────────────────────────────────────────────────────────────
ENTRIES.extend([
 e("lamb_li_150_s1","Lambretta","Li 150","Serie 1","LD","Lambretta",1958,1959,"ROLLER",
   engine={"displacementCc":"148","boreMm":"60","strokeMm":"52","compressionRatio":"6,7:1","coolingType":"Luft","intakeSystem":"Kolbensteuerung"},
   carb={"carbType":"Dell'Orto MA 19","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingSecondary":"3-Gang"}),

 e("lamb_li_150_s2","Lambretta","Li 150","Serie 2","LI","Lambretta",1959,1961,"ROLLER",
   engine={"displacementCc":"148","boreMm":"60","strokeMm":"52","compressionRatio":"6,7:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto MA 19","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung","gearingSecondary":"4-Gang"}),

 e("lamb_li_150_s3","Lambretta","Li 150","Serie 3","LIS3","Lambretta",1961,1967,"ROLLER",
   engine={"displacementCc":"148","boreMm":"60","strokeMm":"52","compressionRatio":"7:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto MA 19","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("lamb_sx_150","Lambretta","SX 150","","SX","Lambretta",1966,1970,"ROLLER",
   engine={"displacementCc":"148","boreMm":"60","strokeMm":"52","compressionRatio":"7,5:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto MA 19","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"}),

 e("lamb_tv_175","Lambretta","TV 175","Serie 3","TV","Lambretta",1962,1965,"ROLLER",
   engine={"displacementCc":"172","boreMm":"62","strokeMm":"57","compressionRatio":"7:1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto MA 19","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"}),

 e("lamb_gp_125","Lambretta","GP 125","Mk1","DL","Lambretta",1969,1971,"ROLLER",
   engine={"displacementCc":"123","boreMm":"52,5","strokeMm":"57","compressionRatio":"8:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SHB 20/20","mainJet":"98","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","clutchType":"Nasskupplung"}),

 e("lamb_gp_150","Lambretta","GP 150","Mk2","GP","Lambretta",1971,1975,"ROLLER",
   engine={"displacementCc":"148","boreMm":"60","strokeMm":"52","compressionRatio":"8:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SHB 20/20","mainJet":"100","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"}),

 e("lamb_gp_200","Lambretta","GP 200","Mk2","GP","Lambretta",1971,1975,"ROLLER",
   engine={"displacementCc":"198","boreMm":"66,5","strokeMm":"57","compressionRatio":"8,5:1","coolingType":"Luft","intakeSystem":"Membran"},
   carb={"carbType":"Dell'Orto SHB 24/24","mainJet":"118","fuelMixRatio":"1:50","sparkPlug":"NGK B7HS","ignitionSystem":"CDI"}),

 e("lamb_j50","Lambretta","J 50","De Luxe","J50","Lambretta",1960,1963,"ROLLER",
   engine={"displacementCc":"49","boreMm":"38","strokeMm":"43","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto MA 12","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"}),

 e("lamb_lui_50","Lambretta","Lui 50","C/CL","LUI","Lambretta",1968,1970,"ROLLER",
   engine={"displacementCc":"49","boreMm":"38","strokeMm":"43","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto MA 12","fuelMixRatio":"1:20","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"}),
])

# ── PUCH / SACHS / HERCULES ──────────────────────────────────────────────────
ENTRIES.extend([
 e("puch_maxi_s","Puch","Maxi S","","","Moped",1974,1989,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","compressionRatio":"6,5:1","coolingType":"Luft","intakeSystem":"Membran Bing"},
   carb={"carbType":"Bing 12/12","mainJet":"52","pilotJet":"35","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionTimingDeg":"2,4","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"52","chainType":"1/2\" × 3/16\""},
   chassis={"frontTire":"2,25-17","rearTire":"2,50-17","frontBrake":"Trommel","rearBrake":"Trommel"},
   elec={"battery":"6V 4 Ah","alternator":"Magnetzünder"}),

 e("puch_maxi_l","Puch","Maxi L","","","Moped",1970,1976,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","compressionRatio":"6,5:1","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","mainJet":"52","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"52"}),

 e("puch_monza_sl","Puch","Monza SL","","","Mokick",1978,1985,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"48"}),

 e("puch_ms50","Puch","MS 50","","","Moped",1965,1970,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"55"}),

 e("sachs_saxonette","Sachs","Saxonette","","","Moped",1978,1995,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"52"}),

 e("sachs_optima_50","Sachs","Optima 50","","","Moped",1970,1985,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("hercules_optima_50","Hercules","Optima 50","","","Moped",1975,1985,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("kreidler_florett_rs","Kreidler","Florett RS","K54","K54","Mokick",1979,1989,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"46"}),

 e("kreidler_mustang","Kreidler","Mustang","K54","K54","Mokick",1970,1978,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),
])

# ── PEUGEOT ────────────────────────────────────────────────────────────────────
ENTRIES.extend([
 e("peugeot_103_sp","Peugeot","103 SP","","","Moped",1974,1990,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Gurtner AR2 12","fuelMixRatio":"1:50","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"54"}),

 e("peugeot_103_lc","Peugeot","103 LC","","","Moped",1980,1990,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Gurtner AR2 12","fuelMixRatio":"1:50","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("peugeot_102","Peugeot","102","","","Moped",1965,1980,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Gurtner AR2 12","fuelMixRatio":"1:50","sparkPlug":"Champion L82","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("peugeot_speedfight_2","Peugeot","Speedfight 2","50 LC","","Scooter 50",2000,2007,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik","clutchType":"Zentrifugalkupplung"}),

 e("peugeot_speedfight_3","Peugeot","Speedfight 3","50 2T","","Scooter 50",2007,2014,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("peugeot_vivacity_50","Peugeot","Vivacity","50 2T","","Scooter 50",1999,2008,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("peugeot_django_125","Peugeot","Django","125 4T","","Maxiscooter",2014,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124,8","boreMm":"52,4","strokeMm":"57,8","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),
])

# ── PIAGGIO / GILERA ───────────────────────────────────────────────────────────
ENTRIES.extend([
 e("piaggio_zip_50_2t","Piaggio","ZIP","50 2T","","Scooter 50",1996,2026,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"39,3","strokeMm":"41,1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("piaggio_zip_50_4t","Piaggio","ZIP","50 4T","","Scooter 50",2005,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"49,9","boreMm":"39","strokeMm":"41,8","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK CR7HSA","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("piaggio_nrg_50","Piaggio","NRG","50 LC DD","","Scooter 50",1994,2005,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("piaggio_typhoon_50","Piaggio","Typhoon","50","","Scooter 50",1993,2004,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"39,3","strokeMm":"41,1","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("piaggio_liberty_125","Piaggio","Liberty","125 4T","","Maxiscooter",2003,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,8","strokeMm":"56,4","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),

 e("gilera_runner_50","Gilera","Runner","50 SP","","Scooter 50",1997,2012,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),
])

# ── MBK / YAMAHA / HONDA ───────────────────────────────────────────────────────
ENTRIES.extend([
 e("mbk_booster_50","MBK","Booster","50","","Scooter 50",1989,2004,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("mbk_nitro_50","MBK","Nitro","50","","Scooter 50",1997,2008,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("yamaha_aerox_50","Yamaha","Aerox","50","","Scooter 50",1997,2026,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Mikuni TM24","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("yamaha_jog_rr","Yamaha","Jog RR","50","","Scooter 50",2004,2016,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Mikuni VM12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("yamaha_bws_125","Yamaha","BWS","125","","Maxiscooter",2010,2020,"ROLLER",
   engine={"displacementCc":"124","boreMm":"52,4","strokeMm":"57,8","coolingType":"Luft"},
   carb={"carbType":"Mikuni BSR37","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("honda_zoomer_50","Honda","Zoomer","50","","Scooter 50",2001,2008,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"39","strokeMm":"41,8","coolingType":"Luft"},
   carb={"carbType":"Keihin PZ20","fuelMixRatio":"1:50","sparkPlug":"NGK CR7HSA","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("honda_sh_125","Honda","SH","125 4T","","Maxiscooter",2001,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,4","strokeMm":"57,8","coolingType":"Flüssig"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),

 e("honda_pcx_125","Honda","PCX","125","","Maxiscooter",2010,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,4","strokeMm":"57,8","coolingType":"Flüssig"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),
])

# ── DERBI / APRILIA / SYM / KTM ────────────────────────────────────────────────
ENTRIES.extend([
 e("derbi_senda_50","Derbi","Senda","50 SM","","Cross",1995,2012,"CROSS",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8ES","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"52"}),

 e("derbi_gpr_50","Derbi","GPR","50 Racing","","Cross",1997,2012,"CROSS",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8ES","ignitionSystem":"CDI"},
   drive={"driveType":"Kette"}),

 e("aprilia_rs_50","Aprilia","RS","50","","Cross",1999,2006,"CROSS",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Dell'Orto PHBN 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8ES","ignitionSystem":"CDI"},
   drive={"driveType":"Kette"}),

 e("aprilia_sportcity_125","Aprilia","Sportcity","125 4T","","Maxiscooter",2007,2016,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,4","strokeMm":"57,8","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),

 e("sym_jet_50","SYM","Jet","50 2T","","Scooter 50",2003,2015,"ROLLER",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Luft"},
   carb={"carbType":"Dell'Orto PHVA 12","fuelMixRatio":"1:50","sparkPlug":"NGK BR8HS","ignitionSystem":"CDI"},
   drive={"driveType":"Variomatik"}),

 e("sym_joyride_125","SYM","Joyride","125 4T","","Maxiscooter",2005,2020,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"124","boreMm":"52,4","strokeMm":"57,8","coolingType":"Luft"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),

 e("ktm_50_sx","KTM","50 SX","","","Cross",2000,2026,"CROSS",
   engine={"displacementCc":"49,9","boreMm":"40","strokeMm":"39,8","coolingType":"Flüssig"},
   carb={"carbType":"Mikuni TM28","fuelMixRatio":"1:50","sparkPlug":"NGK BR8ES","ignitionSystem":"CDI"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"48"}),
])

# ── SIMSON / MZ / ZÜNDAPP ───────────────────────────────────────────────────────
ENTRIES.extend([
 e("simson_s51","Simson","S51","","","Mokick",1980,1990,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette","frontSprocketTeeth":"11","rearSprocketTeeth":"52"}),

 e("simson_s50","Simson","S50","","","Mofa",1975,1980,"MOFA",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("simson_sr50","Simson","SR 50/80","","","Mokick",1986,1990,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("simson_schwalbe","Simson","Schwalbe","KR51/1","KR51","Mokick",1964,1980,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("mz_ts_150","MZ","TS","150","","Mokick",1967,1985,"MOKICK",
   engine={"displacementCc":"143","boreMm":"56","strokeMm":"58","coolingType":"Luft"},
   carb={"carbType":"BVF 22/2","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),

 e("zundapp_cs_50","Zündapp","CS 50","Hydro","","Mokick",1970,1984,"MOKICK",
   engine={"displacementCc":"49,9","boreMm":"38","strokeMm":"43,8","coolingType":"Luft"},
   carb={"carbType":"Bing 12/12","fuelMixRatio":"1:50","sparkPlug":"Bosch W8AC","ignitionSystem":"Kontakt"},
   drive={"driveType":"Kette"}),
])

# ── MAXISCOOTER ─────────────────────────────────────────────────────────────────
ENTRIES.extend([
 e("yamaha_tmax_560","Yamaha","TMAX","560","","Maxiscooter",2020,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"562","boreMm":"70","strokeMm":"73,2","coolingType":"Flüssig","cylinderCount":"2"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik","clutchType":"Wandlerkupplung"},
   chassis={"frontTire":"120/70-15","rearTire":"160/60-15","frontBrake":"Scheibe","rearBrake":"Scheibe"}),

 e("honda_forza_350","Honda","Forza","350","","Maxiscooter",2020,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"329,6","boreMm":"70","strokeMm":"64,8","coolingType":"Flüssig","cylinderCount":"1"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"},
   chassis={"frontTire":"120/70-15","rearTire":"140/70-14"}),

 e("bmw_c400x","BMW","C 400 X","","","Maxiscooter",2018,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"350","boreMm":"72","strokeMm":"60","coolingType":"Flüssig","cylinderCount":"1"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"},
   chassis={"frontTire":"120/70-15","rearTire":"140/70-14"}),

 e("suzuki_burgman_400","Suzuki","Burgman","400","","Maxiscooter",2006,2026,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"399","boreMm":"81","strokeMm":"77,6","coolingType":"Flüssig","cylinderCount":"1"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),

 e("kymco_downtown_300i","KYMCO","Downtown","300i","","Maxiscooter",2010,2020,"ROLLER",cycle="FOUR_STROKE",
   engine={"displacementCc":"299","boreMm":"72","strokeMm":"60","coolingType":"Flüssig"},
   carb={"fuelType":"Super E10","oilType":"4-Takt","sparkPlug":"NGK LMAR8A-9"},
   drive={"driveType":"Variomatik"}),
])

def main():
    catalog = {
        "version": 1,
        "source": "Kuratiert aus SIP Scootershop Modelbase, Scooter Center, Scooter Help und öffentlichen Fahrzeugdatenbanken",
        "entries": ENTRIES,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(catalog, f, ensure_ascii=False, indent=2)
    brands = sorted({e["brand"] for e in ENTRIES})
    print(f"Generated {len(ENTRIES)} entries, {len(brands)} brands -> {OUT}")

if __name__ == "__main__":
    main()
