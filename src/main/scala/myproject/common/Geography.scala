package myproject.common

import java.util.{Currency, Locale}

object Geography {

  case class Address(street: Option[String], zip: Option[String], city: Option[String], country: Option[Country])

  def getAllCountries = Countries.all

  def getCountry(code: String): Option[Country] = {
    Countries.all.find(c => c.iso2 == code || c.iso3 == code)
  }

  def getCountryF(code: String) = getCountry(code).getOrElse(throw ObjectNotFoundException(s"Country with country code `$code` was not found"))

  case class Country(
      iso2: String,
      iso3: String,
      currency: Currency,
      name: String,
      locale: Locale,
      active: Boolean)

  object Country {
    def apply(iso2: String, iso3: String, currencyCode: String, name: String, languageTag: String, active: Boolean = false) = {
      new Country(iso2, iso3, Currency.getInstance(currencyCode), name, Locale.forLanguageTag(languageTag), active)
    }
  }

  val EuropeanCountries = {

    import Countries._

    List(Switzerland, Norway, Liechtenstein, Iceland, UnitedKingdom, Sweden, Spain, Slovenia, Slovakia, Romania,
      Portugal, Poland, Netherlands, Malta, Luxembourg, Lithuania, Latvia, Italy, Ireland, Hungary, Greece, Germany,
      France, Finland, Estonia, Denmark, CzechRepublic, Cyprus, Croatia, Bulgaria, Belgium, Austria)
  }

  object Countries {
    val UnitedArabEmirates = Country("AE","ARE","AED","United Arab Emirates","ar-AE")
    val Afghanistan = Country("AF","AFG","AFN","Afghanistan","und-AF")
    val Andorra = Country("AD", "AND", "EUR", "Afghanistan", "und-AD")
    // val Antarctica = Country("AQ", "ATA", ???, "Antarctica", "und-AQ") // No currency
    val AntiguaAndBarbuda = Country("AG","ATG","XCD","Antigua and Barbuda","und-AG")
    val Anguilla = Country("AI","AIA","XCD","Anguilla","und-AI")
    val Albania = Country("AL","ALB","ALL","Albania","sq-AL")
    val Armenia = Country("AM","ARM","AMD","Armenia","und-AM")
    val NetherlandsAntilles = Country("AN","ANT","ANG","Netherlands Antilles","und-AN")
    val Angola = Country("AO","AGO","AOA","Angola","und-AO")
    val Argentina = Country("AR","ARG","ARS","Argentina","es-AR")
    val AmericanSamoa = Country("AS","ASM","USD","American Samoa","und-AS")
    val Austria = Country("AT","AUT","EUR","Austria","de-AT")
    val Australia = Country("AU","AUS","AUD","Australia","en-AU")
    val Aruba = Country("AW","ABW","AWG","Aruba","und-AW")
    val ÅlandIslands = Country("AX","ALA","EUR","Åland Islands","und-AX")
    val Azerbaijan = Country("AZ","AZE","AZN","Azerbaijan","und-AZ")
    val BosniaAndHerzegovina = Country("BA","BIH","BAM","Bosnia and Herzegovina","sr-BA-#Latn")
    val Barbados = Country("BB","BRB","BBD","Barbados","und-BB")
    val Bangladesh = Country("BD","BGD","BDT","Bangladesh","und-BD")
    val Belgium = Country("BE","BEL","EUR","Belgium","fr-BE")
    val BurkinaFaso = Country("BF","BFA","XOF","Burkina Faso","und-BF")
    val Bulgaria = Country("BG","BGR","BGN","Bulgaria","bg-BG")
    val Bahrain = Country("BH","BHR","BHD","Bahrain","ar-BH")
    val Burundi = Country("BI","BDI","BIF","Burundi","und-BI")
    val Benin = Country("BJ","BEN","XOF","Benin","und-BJ")
    val SaintBarthélemy = Country("BL","BLM","EUR","Saint Barthélemy","und-BL")
    val Bermuda = Country("BM","BMU","BMD","Bermuda","und-BM")
    val Brunei = Country("BN","BRN","BND","Brunei","und-BN")
    val Bolivia = Country("BO","BOL","BOB","Bolivia","es-BO")
    val BonaireSintEustatiusAndSaba = Country("BQ","BES","USD","Bonaire Sint Eustatius and Saba","und-BQ")
    val Brazil = Country("BR","BRA","BRL","Brazil","pt-BR")
    val Bahamas = Country("BS","BHS","BSD","Bahamas","und-BS")
    val Bhutan = Country("BT","BTN","BTN","Bhutan","und-BT")
    val BouvetIsland = Country("BV","BVT","NOK","Bouvet Island","und-BV")
    val Botswana = Country("BW","BWA","BWP","Botswana","und-BW")
    val Belarus = Country("BY","BLR","BYR","Belarus","be-BY")
    val Belize = Country("BZ","BLZ","BZD","Belize","und-BZ")
    val Canada = Country("CA","CAN","CAD","Canada","fr-CA")
    val CocosIslands = Country("CC","CCK","AUD","Cocos Islands","und-CC")
    val DemocraticRepublicOfCongo = Country("CD","COD","CDF","The Democratic Republic Of Congo","und-CD")
    val CentralAfricanRepublic = Country("CF","CAF","XAF","Central African Republic","und-CF")
    val Congo = Country("CG","COG","XAF","Congo","und-CG")
    val Switzerland = Country("CH","CHE","CHF","Switzerland","fr-CH")
    val IvoryCoast = Country("CI","CIV","XOF","Côte d'Ivoire","und-CI")
    val CookIslands = Country("CK","COK","NZD","Cook Islands","und-CK")
    val Chile = Country("CL","CHL","CLP","Chile","es-CL")
    val Cameroon = Country("CM","CMR","XAF","Cameroon","und-CM")
    val China = Country("CN","CHN","CNY","China","zh-CN")
    val Colombia = Country("CO","COL","COP","Colombia","es-CO")
    val CostaRica = Country("CR","CRI","CRC","Costa Rica","es-CR")
    val Cuba = Country("CU","CUB","CUP","Cuba","es-CU")
    val CapeVerde = Country("CV","CPV","CVE","Cape Verde","und-CV")
    val Curaçao = Country("CW","CUW","ANG","Curaçao","und-CW")
    val ChristmasIsland = Country("CX","CXR","AUD","Christmas Island","und-CX")
    val Cyprus = Country("CY","CYP","EUR","Cyprus","el-CY")
    val CzechRepublic = Country("CZ","CZE","CZK","Czech Republic","cs-CZ")
    val Germany = Country("DE","DEU","EUR","Germany","de-DE")
    val Djibouti = Country("DJ","DJI","DJF","Djibouti","und-DJ")
    val Denmark = Country("DK","DNK","DKK","Denmark","da-DK")
    val Dominica = Country("DM","DMA","XCD","Dominica","und-DM")
    val DominicanRepublic = Country("DO","DOM","DOP","Dominican Republic","es-DO")
    val Algeria = Country("DZ","DZA","DZD","Algeria","ar-DZ")
    val Ecuador = Country("EC","ECU","USD","Ecuador","es-EC")
    val Estonia = Country("EE","EST","EUR","Estonia","et-EE")
    val Egypt = Country("EG","EGY","EGP","Egypt","ar-EG")
    val WesternSahara = Country("EH","ESH","MAD","Western Sahara","und-EH")
    val Eritrea = Country("ER","ERI","ERN","Eritrea","und-ER")
    val Spain = Country("ES","ESP","EUR","Spain","ca-ES")
    val Ethiopia = Country("ET","ETH","ETB","Ethiopia","und-ET")
    val Finland = Country("FI","FIN","EUR","Finland","fi-FI")
    val Fiji = Country("FJ","FJI","FJD","Fiji","und-FJ")
    val FalklandIslands = Country("FK","FLK","FKP","Falkland Islands","und-FK")
    val Micronesia = Country("FM","FSM","USD","Micronesia","und-FM")
    val FaroeIslands = Country("FO","FRO","DKK","Faroe Islands","und-FO")
    val France = Country("FR","FRA","EUR","France","fr-FR")
    val Gabon = Country("GA","GAB","XAF","Gabon","und-GA")
    val UnitedKingdom = Country("GB","GBR","GBP","United Kingdom","en-GB")
    val Grenada = Country("GD","GRD","XCD","Grenada","und-GD")
    val Georgia = Country("GE","GEO","GEL","Georgia","und-GE")
    val FrenchGuiana = Country("GF","GUF","EUR","French Guiana","und-GF")
    val Guernsey = Country("GG","GGY","GBP","Guernsey","und-GG")
    val Ghana = Country("GH","GHA","GHS","Ghana","und-GH")
    val Gibraltar = Country("GI","GIB","GIP","Gibraltar","und-GI")
    val Greenland = Country("GL","GRL","DKK","Greenland","und-GL")
    val Gambia = Country("GM","GMB","GMD","Gambia","und-GM")
    val Guinea = Country("GN","GIN","GNF","Guinea","und-GN")
    val Guadeloupe = Country("GP","GLP","EUR","Guadeloupe","und-GP")
    val EquatorialGuinea = Country("GQ","GNQ","XAF","Equatorial Guinea","und-GQ")
    val Greece = Country("GR","GRC","EUR","Greece","el-GR")
    val SouthGeorgiaAndTheSouthSandwichIslands = Country("GS","SGS","GBP","South Georgia And The South Sandwich Islands","und-GS")
    val Guatemala = Country("GT","GTM","GTQ","Guatemala","es-GT")
    val Guam = Country("GU","GUM","USD","Guam","und-GU")
    val GuineaBissau = Country("GW","GNB","XOF","Guinea-Bissau","und-GW")
    val Guyana = Country("GY","GUY","GYD","Guyana","und-GY")
    val HongKong = Country("HK","HKG","HKD","Hong Kong","zh-HK")
    val HeardIslandAndMcDonaldIslands = Country("HM","HMD","AUD","Heard Island And McDonald Islands","und-HM")
    val Honduras = Country("HN","HND","HNL","Honduras","es-HN")
    val Croatia = Country("HR","HRV","HRK","Croatia","hr-HR")
    val Haiti = Country("HT","HTI","HTG","Haiti","und-HT")
    val Hungary = Country("HU","HUN","HUF","Hungary","hu-HU")
    val Indonesia = Country("ID","IDN","IDR","Indonesia","in-ID")
    val Ireland = Country("IE","IRL","EUR","Ireland","ga-IE")
    val Israel = Country("IL","ISR","ILS","Israel","iw-IL")
    val IsleOfMan = Country("IM","IMN","GBP","Isle Of Man","und-IM")
    val India = Country("IN","IND","INR","India","hi-IN")
    val BritishIndianOceanTerritory = Country("IO","IOT","USD","British Indian Ocean Territory","und-IO")
    val Iraq = Country("IQ","IRQ","IQD","Iraq","ar-IQ")
    val Iran = Country("IR","IRN","IRR","Iran","und-IR")
    val Iceland = Country("IS","ISL","ISK","Iceland","is-IS")
    val Italy = Country("IT","ITA","EUR","Italy","it-IT")
    val Jersey = Country("JE","JEY","GBP","Jersey","und-JE")
    val Jamaica = Country("JM","JAM","JMD","Jamaica","und-JM")
    val Jordan = Country("JO","JOR","JOD","Jordan","ar-JO")
    val Japan = Country("JP","JPN","JPY","Japan","ja-JP-JP-#u-ca-japanese")
    val Kenya = Country("KE","KEN","KES","Kenya","und-KE")
    val Kyrgyzstan = Country("KG","KGZ","KGS","Kyrgyzstan","und-KG")
    val Cambodia = Country("KH","KHM","KHR","Cambodia","und-KH")
    val Kiribati = Country("KI","KIR","AUD","Kiribati","und-KI")
    val Comoros = Country("KM","COM","KMF","Comoros","und-KM")
    val SaintKittsAndNevis = Country("KN","KNA","XCD","Saint Kitts And Nevis","und-KN")
    val NorthKorea = Country("KP","PRK","KPW","North Korea","und-KP")
    val SouthKorea = Country("KR","KOR","KRW","South Korea","ko-KR")
    val Kuwait = Country("KW","KWT","KWD","Kuwait","ar-KW")
    val CaymanIslands = Country("KY","CYM","KYD","Cayman Islands","und-KY")
    val Kazakhstan = Country("KZ","KAZ","KZT","Kazakhstan","und-KZ")
    val Laos = Country("LA","LAO","LAK","Laos","und-LA")
    val Lebanon = Country("LB","LBN","LBP","Lebanon","ar-LB")
    val SaintLucia = Country("LC","LCA","XCD","Saint Lucia","und-LC")
    val Liechtenstein = Country("LI","LIE","CHF","Liechtenstein","und-LI")
    val SriLanka = Country("LK","LKA","LKR","Sri Lanka","und-LK")
    val Liberia = Country("LR","LBR","LRD","Liberia","und-LR")
    val Lesotho = Country("LS","LSO","LSL","Lesotho","und-LS")
    val Lithuania = Country("LT","LTU","EUR","Lithuania","lt-LT")
    val Luxembourg = Country("LU","LUX","EUR","Luxembourg","fr-LU")
    val Latvia = Country("LV","LVA","EUR","Latvia","lv-LV")
    val Libya = Country("LY","LBY","LYD","Libya","ar-LY")
    val Morocco = Country("MA","MAR","MAD","Morocco","ar-MA")
    val Monaco = Country("MC","MCO","EUR","Monaco","und-MC")
    val Moldova = Country("MD","MDA","MDL","Moldova","und-MD")
    val Montenegro = Country("ME","MNE","EUR","Montenegro","sr-ME")
    val SaintMartin = Country("MF","MAF","EUR","Saint Martin","und-MF")
    val Madagascar = Country("MG","MDG","MGA","Madagascar","und-MG")
    val MarshallIslands = Country("MH","MHL","USD","Marshall Islands","und-MH")
    val Macedonia = Country("MK","MKD","MKD","Macedonia","mk-MK")
    val Mali = Country("ML","MLI","XOF","Mali","und-ML")
    val Myanmar = Country("MM","MMR","MMK","Myanmar","und-MM")
    val Mongolia = Country("MN","MNG","MNT","Mongolia","und-MN")
    val Macao = Country("MO","MAC","MOP","Macao","und-MO")
    val NorthernMarianaIslands = Country("MP","MNP","USD","Northern Mariana Islands","und-MP")
    val Martinique = Country("MQ","MTQ","EUR","Martinique","und-MQ")
    val Mauritania = Country("MR","MRT","MRO","Mauritania","und-MR")
    val Montserrat = Country("MS","MSR","XCD","Montserrat","und-MS")
    val Malta = Country("MT","MLT","EUR","Malta","mt-MT")
    val Mauritius = Country("MU","MUS","MUR","Mauritius","und-MU")
    val Maldives = Country("MV","MDV","MVR","Maldives","und-MV")
    val Malawi = Country("MW","MWI","MWK","Malawi","und-MW")
    val Mexico = Country("MX","MEX","MXN","Mexico","es-MX")
    val Malaysia = Country("MY","MYS","MYR","Malaysia","ms-MY")
    val Mozambique = Country("MZ","MOZ","MZN","Mozambique","und-MZ")
    val Namibia = Country("NA","NAM","NAD","Namibia","und-NA")
    val NewCaledonia = Country("NC","NCL","XPF","New Caledonia","und-NC")
    val Niger = Country("NE","NER","XOF","Niger","und-NE")
    val NorfolkIsland = Country("NF","NFK","AUD","Norfolk Island","und-NF")
    val Nigeria = Country("NG","NGA","NGN","Nigeria","und-NG")
    val Nicaragua = Country("NI","NIC","NIO","Nicaragua","es-NI")
    val Netherlands = Country("NL","NLD","EUR","Netherlands","nl-NL")
    val Norway = Country("NO","NOR","NOK","Norway","nn-NO")
    val Nepal = Country("NP","NPL","NPR","Nepal","und-NP")
    val Nauru = Country("NR","NRU","AUD","Nauru","und-NR")
    val Niue = Country("NU","NIU","NZD","Niue","und-NU")
    val NewZealand = Country("NZ","NZL","NZD","New Zealand","en-NZ")
    val Oman = Country("OM","OMN","OMR","Oman","ar-OM")
    val Panama = Country("PA","PAN","PAB","Panama","es-PA")
    val Peru = Country("PE","PER","PEN","Peru","es-PE")
    val FrenchPolynesia = Country("PF","PYF","XPF","French Polynesia","und-PF")
    val PapuaNewGuinea = Country("PG","PNG","PGK","Papua New Guinea","und-PG")
    val Philippines = Country("PH","PHL","PHP","Philippines","en-PH")
    val Pakistan = Country("PK","PAK","PKR","Pakistan","und-PK")
    val Poland = Country("PL","POL","PLN","Poland","pl-PL")
    val SaintPierreAndMiquelon = Country("PM","SPM","EUR","Saint Pierre And Miquelon","und-PM")
    val Pitcairn = Country("PN","PCN","NZD","Pitcairn","und-PN")
    val PuertoRico = Country("PR","PRI","USD","Puerto Rico","es-PR")
    val Palestine = Country("PS","PSE","ILS","Palestine","und-PS")
    val Portugal = Country("PT","PRT","EUR","Portugal","pt-PT")
    val Palau = Country("PW","PLW","USD","Palau","und-PW")
    val Paraguay = Country("PY","PRY","PYG","Paraguay","es-PY")
    val Qatar = Country("QA","QAT","QAR","Qatar","ar-QA")
    val Reunion = Country("RE","REU","EUR","Reunion","und-RE")
    val Romania = Country("RO","ROU","RON","Romania","ro-RO")
    val Serbia = Country("RS","SRB","RSD","Serbia","sr-RS")
    val Russia = Country("RU","RUS","RUB","Russia","ru-RU")
    val Rwanda = Country("RW","RWA","RWF","Rwanda","und-RW")
    val SaudiArabia = Country("SA","SAU","SAR","Saudi Arabia","ar-SA")
    val SolomonIslands = Country("SB","SLB","SBD","Solomon Islands","und-SB")
    val Seychelles = Country("SC","SYC","SCR","Seychelles","und-SC")
    val Sudan = Country("SD","SDN","SDG","Sudan","ar-SD")
    val Sweden = Country("SE","SWE","SEK","Sweden","sv-SE")
    val Singapore = Country("SG","SGP","SGD","Singapore","en-SG")
    val SaintHelena = Country("SH","SHN","SHP","Saint Helena","und-SH")
    val Slovenia = Country("SI","SVN","EUR","Slovenia","sl-SI")
    val SvalbardAndJanMayen = Country("SJ","SJM","NOK","Svalbard And Jan Mayen","und-SJ")
    val Slovakia = Country("SK","SVK","EUR","Slovakia","sk-SK")
    val SierraLeone = Country("SL","SLE","SLL","Sierra Leone","und-SL")
    val SanMarino = Country("SM","SMR","EUR","San Marino","und-SM")
    val Senegal = Country("SN","SEN","XOF","Senegal","und-SN")
    val Somalia = Country("SO","SOM","SOS","Somalia","und-SO")
    val Suriname = Country("SR","SUR","SRD","Suriname","und-SR")
    val SouthSudan = Country("SS","SSD","SSP","South Sudan","und-SS")
    val SaoTomeAndPrincipe = Country("ST","STP","STD","Sao Tome And Principe","und-ST")
    val ElSalvador = Country("SV","SLV","SVC","El Salvador","es-SV")
    val SintMaarten = Country("SX","SXM","ANG","Sint Maarten (Dutch part)", "und-SX")
    val Syria = Country("SY","SYR","SYP","Syria","ar-SY")
    val Swaziland = Country("SZ","SWZ","SZL","Swaziland","und-SZ")
    val TurksAndCaicosIslands = Country("TC","TCA","USD","Turks And Caicos Islands","und-TC")
    val Chad = Country("TD","TCD","XAF","Chad","und-TD")
    val FrenchSouthernTerritories = Country("TF","ATF","EUR","French Southern Territories","und-TF")
    val Togo = Country("TG","TGO","XOF","Togo","und-TG")
    val Thailand = Country("TH","THA","THB","Thailand","th-TH")
    val Tajikistan = Country("TJ","TJK","TJS","Tajikistan","und-TJ")
    val Tokelau = Country("TK","TKL","NZD","Tokelau","und-TK")
    val TimorLeste = Country("TL","TLS","USD","Timor-Leste","und-TL")
    val Turkmenistan = Country("TM","TKM","TMT","Turkmenistan","und-TM")
    val Tunisia = Country("TN","TUN","TND","Tunisia","ar-TN")
    val Tonga = Country("TO","TON","TOP","Tonga","und-TO")
    val Turkey = Country("TR","TUR","TRY","Turkey","tr-TR")
    val TrinidadAndTobago = Country("TT","TTO","TTD","Trinidad and Tobago","und-TT")
    val Tuvalu = Country("TV","TUV","AUD","Tuvalu","und-TV")
    val Taiwan = Country("TW","TWN","TWD","Taiwan","zh-TW")
    val Tanzania = Country("TZ","TZA","TZS","Tanzania","und-TZ")
    val Ukraine = Country("UA","UKR","UAH","Ukraine","uk-UA")
    val Uganda = Country("UG","UGA","UGX","Uganda","und-UG")
    val UnitedStatesMinorOutlyingIslands = Country("UM","UMI","USD","United States Minor Outlying Islands","und-UM")
    val UnitedStates = Country("US","USA","USD","United States","en-US")
    val Uruguay = Country("UY","URY","UYU","Uruguay","es-UY")
    val Uzbekistan = Country("UZ","UZB","UZS","Uzbekistan","und-UZ")
    val Vatican = Country("VA","VAT","EUR","Vatican","und-VA")
    val SaintVincentAndTheGrenadines = Country("VC","VCT","XCD","Saint Vincent And The Grenadines","und-VC")
    val Venezuela = Country("VE","VEN","VEF","Venezuela","es-VE")
    val BritishVirginIslands = Country("VG","VGB","USD","British Virgin Islands","und-VG")
    val VirginIslands = Country("VI","VIR","USD","U.S. Virgin Islands","und-VI")
    val Vietnam = Country("VN","VNM","VND","Vietnam","vi-VN")
    val Vanuatu = Country("VU","VUT","VUV","Vanuatu","und-VU")
    val WallisAndFutuna = Country("WF","WLF","XPF","Wallis And Futuna","und-WF")
    val Samoa = Country("WS","WSM","WST","Samoa","und-WS")
    val Yemen = Country("YE","YEM","YER","Yemen","ar-YE")
    val Mayotte = Country("YT","MYT","EUR","Mayotte","und-YT")
    val SouthAfrica = Country("ZA","ZAF","ZAR","South Africa","en-ZA")
    val Zambia = Country("ZM","ZMB","ZMW","Zambia","und-ZM")
    val Zimbabwe = Country("ZW","ZWE","ZWL","Zimbabwe","und-ZW")

    val all = Vector(
      UnitedArabEmirates, Afghanistan, Andorra, AntiguaAndBarbuda, Anguilla, Albania, Armenia,
      NetherlandsAntilles, Angola, Argentina, AmericanSamoa, Austria, Australia,
      Aruba, ÅlandIslands, Azerbaijan, BosniaAndHerzegovina, Barbados, Bangladesh,
      Belgium, BurkinaFaso, Bulgaria, Bahrain, Burundi, Benin, SaintBarthélemy,
      Bermuda, Brunei, Bolivia, BonaireSintEustatiusAndSaba, Brazil, Bahamas, Bhutan,
      BouvetIsland, Botswana, Belarus, Belize, Canada, CocosIslands,
      DemocraticRepublicOfCongo, CentralAfricanRepublic, Congo, Switzerland,
      IvoryCoast, CookIslands, Chile, Cameroon, China, Colombia, CostaRica, Cuba,
      CapeVerde, Curaçao, ChristmasIsland, Cyprus, CzechRepublic, Germany, Djibouti,
      Denmark, Dominica, DominicanRepublic, Algeria, Ecuador, Estonia, Egypt,
      WesternSahara, Eritrea, Spain, Ethiopia, Finland, Fiji, FalklandIslands,
      Micronesia, FaroeIslands, France, Gabon, UnitedKingdom, Grenada, Georgia,
      FrenchGuiana, Guernsey, Ghana, Gibraltar, Greenland, Gambia, Guinea, Guadeloupe,
      EquatorialGuinea, Greece, SouthGeorgiaAndTheSouthSandwichIslands, Guatemala,
      Guam, GuineaBissau, Guyana, HongKong, HeardIslandAndMcDonaldIslands, Honduras,
      Croatia, Haiti, Hungary, Indonesia, Ireland, Israel, IsleOfMan, India,
      BritishIndianOceanTerritory, Iraq, Iran, Iceland, Italy, Jersey, Jamaica,
      Jordan, Japan, Kenya, Kyrgyzstan, Cambodia, Kiribati, Comoros, SaintKittsAndNevis,
      NorthKorea, SouthKorea, Kuwait, CaymanIslands, Kazakhstan, Laos, Lebanon,
      SaintLucia, Liechtenstein, SriLanka, Liberia, Lesotho, Lithuania, Luxembourg,
      Latvia, Libya, Morocco, Monaco, Moldova, Montenegro, SaintMartin, Madagascar,
      MarshallIslands, Macedonia, Mali, Myanmar, Mongolia, Macao,
      NorthernMarianaIslands, Martinique, Mauritania, Montserrat, Malta, Mauritius,
      Maldives, Malawi, Mexico, Malaysia, Mozambique, Namibia, NewCaledonia, Niger,
      NorfolkIsland, Nigeria, Nicaragua, Netherlands, Norway, Nepal, Nauru, Niue,
      NewZealand, Oman, Panama, Peru, FrenchPolynesia, PapuaNewGuinea, Philippines,
      Pakistan, Poland, SaintPierreAndMiquelon, Pitcairn, PuertoRico, Palestine,
      Portugal, Palau, Paraguay, Qatar, Reunion, Romania, Serbia, Russia, Rwanda,
      SaudiArabia, SolomonIslands, Seychelles, Sudan, Sweden, Singapore, SaintHelena,
      Slovenia, SvalbardAndJanMayen, Slovakia, SierraLeone, SanMarino, Senegal,
      Somalia, Suriname, SouthSudan, SaoTomeAndPrincipe, ElSalvador, SintMaarten,
      Syria, Swaziland, TurksAndCaicosIslands, Chad, FrenchSouthernTerritories, Togo,
      Thailand, Tajikistan, Tokelau, TimorLeste, Turkmenistan, Tunisia, Tonga, Turkey,
      TrinidadAndTobago, Tuvalu, Taiwan, Tanzania, Ukraine, Uganda,
      UnitedStatesMinorOutlyingIslands, UnitedStates, Uruguay, Uzbekistan, Vatican,
      SaintVincentAndTheGrenadines, Venezuela, BritishVirginIslands, VirginIslands,
      Vietnam, Vanuatu, WallisAndFutuna, Samoa, Yemen, Mayotte, SouthAfrica, Zambia,
      Zimbabwe)
  }
}

