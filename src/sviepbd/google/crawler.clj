(ns sviepbd.google.crawler 
  (:require [org.httpkit.client :as http]
            [clojure.core.async :as a]
            [clojure.string :as s]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [ring.util.codec :as codec]
            [clojure.java.io :as io]
            [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [monger.core :as mg]
            [monger.collection :as mc]
            [clojure.java.jdbc :as sql]
            [korma.db :as db]
            [korma.core :as k]
            [clojure.data.csv :as csv]
            
            [sviepbd.utils.nlp :as nlpu]
            )
  (:use sviepbd.utils.generic
        sviepbd.utils.scraping
        clojure.repl clojure.pprint))

;; ----------------------------------------------------------------
;; Result page scraping
;; ----------------------------------------------------------------

(comment
  ;; calling google
  (-> @(http/get "http://www.google.com/search" {:query-params {"q" (s/join "+" ["mining" "mythe"])
                                                                "start" 30, "lr" "lang_en"}}) 
    :body)
  (do 
    (def query-result-html "<!doctype html><html itemscope=\"\" itemtype=\"http://schema.org/WebPage\" lang=\"en\"><head><meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"><meta content=\"/images/google_favicon_128.png\" itemprop=\"image\"><title>mining+mythe - Google Search</title><style>#gbar,#guser{font-size:13px;padding-top:1px !important;}#gbar{height:22px}#guser{padding-bottom:7px !important;text-align:right}.gbh,.gbd{border-top:1px solid #c9d7f1;font-size:1px}.gbh{height:0;position:absolute;top:24px;width:100%}@media all{.gb1{height:22px;margin-right:.5em;vertical-align:top}#gbar{float:left}}a.gb1,a.gb4{text-decoration:underline !important}a.gb1,a.gb4{color:#00c !important}.gbi .gb4{color:#dd8e27 !important}.gbf .gb4{color:#900 !important}</style><style>.star{float:left;margin-top:1px;overflow:hidden}._yhd{font-size:11px}.j{width:34em}body,td,div,.p,a{font-family:arial,sans-serif;tap-highlight-color:rgba(255,255,255,0)}body{margin:0}#gbar{float:left;height:22px;padding-left:2px;font-size:13px}.gsfi,.gsfs{font-size:17px}.w,.q:active,.q:visited,.tbotu{color:#11c}a.gl{text-decoration:none}._Umd a:link{color:#0E1CB3}#foot{padding:0 8px}#foot a{white-space:nowrap}h3{font-size:16px;font-weight:normal;margin:0;padding:0}#res h3{display:inline}.hd{height:1px;position:absolute;top:-1000em}li.g,body,html,table,.std{font-size:13px}li.g{margin-bottom:23px;margin-top:0;zoom:1}ol li,ul li{list-style:none}h1,ol,ul,li{margin:0;padding:0}#mbEnd h2{font-weight:normal}.e{margin:2px 0 0.75em}#leftnav a,.slk a{text-decoration:none}#leftnav h2{color:#767676;font-weight:normal;margin:0}#nav{border-collapse:collapse;margin-top:17px;text-align:left}#nav td{text-align:center}.nobr{white-space:nowrap}.ts{border-collapse:collapse}.s br{display:none}.csb{display:block;height:40px}.images_table td{line-height:17px;padding-bottom:16px}.images_table img{border:1px solid #ccc;padding:1px}#tbd,#abd{display:block;min-height:1px}#abd{padding-top:3px}#tbd li{display:inline}._ITd,._JTd{margin-bottom:8px}#tbd .tbt li{display:block;font-size:13px;line-height:1.2;padding-bottom:3px;padding-left:8px;text-indent:-8px}.tbos,.b{font-weight:bold}em{font-weight:bold;font-style:normal}.mime{color:#12c;font-weight:bold;font-size:x-small}._lwd{right:-2px !important;overflow:hidden}.soc a{text-decoration:none}.soc{color:#808080}._AC a{text-decoration:none}._AC{color:#808080}._kgd{color:#e7711b}#_vBb{border:1px solid #e0e0e0;margin-left:-8px;margin-right:-8px;padding:15px 20px 5px}._m3b{font-size:32px}._eGc{color:#777;font-size:16px;margin-top:5px}._H0d{color:#777;font-size:14px;margin-top:5px}#vob{border:1px solid #e0e0e0;padding:15px 15px}#_Nyc{font-size:22px;line-height:22px;padding-bottom:5px}#vob_st{line-height:1.24}._Tsb{border-width:1px;border-style:solid;border-color:#eee;background-color:#fff;position:relative;margin-bottom:26px}._Peb,._Qeb,._Usb{font-family:Arial;font-weight:lighter}._Peb{margin-bottom:5px}._Peb{font-size:xx-large}._Qeb{font-size:medium}._Usb{font-size:small}._Tsb{margin-left:-8px;margin-right:-15px;padding:20px 20px 24px}._rOc{border-spacing:0px 2px}._sOc{max-width:380px;text-overflow:ellipsis;white-space:nowrap;overflow:hidden;padding-left:0px}._v9b{padding-left:15px;white-space:nowrap;color:#666}._pOc{padding-left:0px}._rkc{color:#212121}._HOb{color:#878787}.gssb_a{padding:0 10px !important}.gssb_c{left:132px !important;right:295px !important;top:78px !important;width:572px !important}.gssb_c table{font-size:16px !important}.gssb_e{border:1px solid #ccc !important;border-top-color:#d9d9d9 !important}.gssb_i{background:#eee !important}#res{padding:0 8px}#_FQd{padding:0 8px}#subform_ctrl{font-size:11px;height:17px;margin:5px 3px 0 17px}.taf{padding-bottom:3px}._chd{padding:20px 0 3px}._bhd{padding:20px 0 3px}.slk .sld{width:250px}.slk{margin-bottom:-3px}.slk ._z3b{padding-bottom:5px;width:250px}._QPd{margin-top:1px;margin-bottom:-11px}._zuc{color:#545454}._Auc{padding-top:2px;padding-bottom:1px}._Buc{padding-top:1px;margin-bottom:14px}.ac,.st{line-height:1.24}.mfr,#ofr{font-size:16px;margin:1em 0;padding:0 8px}.s{color:#545454}.ac,._JEe{color:#545454}a.fl,._cD a,.osl a{color:#12c;text-decoration:none}a:link{color:#1a0dab;cursor:pointer}div#tads a:link{color:#1a0dab}div#tads .soc a:link{color:#808080}div#tads ._AC a:link{color:#808080}._AC a:link{color:#808080}._AC a:visited{color:#808080}._AC a:hover{color:#808080;text-decoration:underline}a:visited{color:#61C}.blg a{text-decoration:none}cite,cite a:link{color:#006621;font-style:normal}div#tads cite{color:#006621}.kv{font-size:15px}.kvs{margin-top:1px}.kv,.kvs,.slp{display:block;margin-bottom:1px}.kt{border-spacing:2px 0;margin-top:1px}#mbEnd li{margin:20px 8px 0 0}.f{color:#808080}._pJb{color:#093}h4.r{display:inline;font-size:small;font-weight:normal}li{line-height:1.2}._sPb{display:inline-block;vertical-align:top;overflow:hidden;position:relative}._Gnc{margin:0 0 2em 1.3em}._Gnc li{list-style-type:disc}.osl{color:#777;margin-top:4px}.r{font-size:16px;margin:0}.spell{font-size:16px}.spell_orig{font-size:13px}.spell_orig a{text-decoration:none}.spell_orig b i{font-style:normal;font-weight:normal}.th{border:1px solid #ebebeb}.ts td{padding:0}.videobox{padding-bottom:3px}#leftnav a:hover,#leftnav .tbou a:hover,.slk h3 a,a:hover{text-decoration:underline}#mn{table-layout:fixed;width:100%}#leftnav a{color:#222;font-size:13px}#leftnav{padding:43px 4px 4px 0}.tbos{color:#dd4b39}._AEd{border-top:1px solid #efefef;font-size:13px;margin:10px 0 14px 10px;padding:0}.tbt{margin-bottom:28px}#tbd{padding:0 0 0 16px}.tbou a{color:#222}#center_col{border:0;padding:0 8px 0 0}#topstuff .e{padding-top:3px}#topstuff .sp_cnt{padding-top:6px}#ires{padding-top:6px}#ab_name{color:#dd4b39;font:20px \"Arial\";margin-left:15px}._fld{border-bottom:1px solid #dedede;height:56px;padding-top:1px}#resultStats{color:#999;font-size:13px;overflow:hidden;white-space:nowrap}.mslg>td{padding-right:1px;padding-top:2px}.slk .sld{margin-top:2px;padding:5px 0 5px 5px}._Mvd,.fmp{padding-top:3px}.close_btn{overflow:hidden}#fll a,#bfl a{color:#12c !important;margin:0 12px;text-decoration:none !important}.ng{color:#dd4b39}#_gfb .b{text-decoration:underline}#mss{margin:.33em 0 0;padding:0;display:table}._mY{display:inline-block;float:left;white-space:nowrap;padding-right:16px}#mss p{margin:0;padding-top:5px}.tn{border-bottom:1px solid #ebebeb;display:block;float:left;height:59px;line-height:54px;min-width:980px;padding:0;position:relative;white-space:nowrap}._UXb,a._UXb{color:#777;cursor:pointer;display:inline-block;font-family:arial,sans-serif;font-size:small;height:54px;line-height:54px;margin:0 8px;padding:0 8px;text-decoration:none;white-space:nowrap}._Ihd{border-bottom:3px solid #dd4b39;color:#dd4b39;font-weight:bold;margin:2px 8px 0}a._Jhd:hover{color:black;text-decoration:none;white-space:nowrap}body{margin:0;padding:0}._sxc{display:inline-block;float:left;margin-top:2px}._Hhd,a._Hhd{margin-left:1px}.sd{line-height:43px;padding:0 8px 0 9px}a:active,.osl a:active,.tbou a:active,#leftnav a:active{color:#dd4b39}#_Xud a:active,#bfl a:active{color:#dd4b39 !important}.csb{background:url(/images/nav_logo176.png) no-repeat;overflow:hidden}.close_btn{background:url(/images/nav_logo176.png) no-repeat -138px -84px;height:14px;width:14px;display:block}.star{background:url(/images/nav_logo176.png) no-repeat -94px -245px;height:13px;width:65px;display:block}.star div,.star span{background:url(/images/nav_logo176.png) no-repeat 0 -245px;height:13px;width:65px;display:block}._nBb{display:inline;margin:0 3px;outline-color:transparent;overflow:hidden;position:relative}._nBb>div{outline-color:transparent}._O0{border-color:transparent;border-style:solid dashed dashed;border-top-color:green;border-width:4px 4px 0 4px;cursor:pointer;display:inline-block;font-size:0;height:0;left:4px;line-height:0;outline-color:transparent;position:relative;top:-3px;width:0}._O0{margin-top:-4px}.am-dropdown-menu{display:block;background:#fff;border:1px solid #dcdcdc;font-size:13px;left:0;padding:0;position:absolute;right:auto;white-space:nowrap;z-index:3}._Ykb{list-style:none;white-space:nowrap}._Ykb:hover{background-color:#eee}a._Zkb{color:#333;cursor:pointer;display:block;padding:7px 18px;text-decoration:none}div#tads a._Zkb{color:#333}.sfbgg{background:#f1f1f1;border-bottom:1px solid #e5e5e5;height:71px}#logocont{z-index:1;padding-left:4px;padding-top:4px}#logo{display:block;height:49px;margin-top:12px;margin-left:12px;overflow:hidden;position:relative;width:137px}#logo img{left:0;position:absolute;top:-41px}.lst-a{background:white;border:1px solid #d9d9d9;border-top-color:silver;width:570px}.lst-a:hover{border:1px solid #b9b9b9;border-top:1px solid #a0a0a0;box-shadow:inset 0 1px 2px rgba(0,0,0,0.1);-webkit-box-shadow:inset 0 1px 2px rgba(0,0,0,0.1);-moz-box-shadow:inset 0 1px 2px rgba(0,0,0,0.1)}.lst-td{border:none;padding:0}.tia input{border-right:none;padding-right:0}.tia{padding-right:0}.lst{background:none;border:none;color:#000;font:16px arial,sans-serif;float:left;height:22px;margin:0;padding:3px 6px 2px 9px;vertical-align:top;width:100%;word-break:break-all}.lst:focus{outline:none}.lst-b{background:none;border:none;height:26px;padding:0 6px 0 12px}.ds{border-right:1px solid #e7e7e7;position:relative;height:29px;margin-left:17px;z-index:100}.lsbb{background-image:-moz-linear-gradient(top,#4d90fe,#4787ed);background-image:-ms-linear-gradient(top,#4d90fe,#4787ed);background-image:-o-linear-gradient(top,#4d90fe,#4787ed);background-image:-webkit-gradient(linear,left top,left bottom,from(#4d90fe),to(#4787ed));background-image:-webkit-linear-gradient(top,#4d90fe,#4787ed);background-image:linear-gradient(top,#4d90fe,#4787ed);border:1px solid #3079ed;border-radius:2px;background-color:#4d90fe;height:27px;width:68px}.lsbb:hover{background-image:-moz-linear-gradient(top,#4d90fe,#357ae8);background-image:-ms-linear-gradient(top,#4d90fe,#357ae8);background-image:-o-linear-gradient(top,#4d90fe,#357ae8);background-image:-webkit-gradient(linear,left top,left bottom,from(#4d90fe),to(#357ae8));background-image:-webkit-linear-gradient(top,#4d90fe,#357ae8);background-color:#357ae8;background-image:linear-gradient(top,#4d90fe,#357ae8);border:1px solid #2f5bb7}.lsb{background:transparent;background-position:0 -343px;background-repeat:repeat-x;border:none;color:#000;cursor:default;font:15px arial,sans-serif;height:29px;margin:0;vertical-align:top;width:100%}.lsb:active{-moz-box-shadow:inset 0 1px 2px rgba(0,0,0,0.3);-webkit-box-shadow:inset 0 1px 2px rgba(0,0,0,0.3);box-shadow:inset 0 1px 2px rgba(0,0,0,0.3);background:transparent;color:transparent;overflow:hidden;position:relative;width:100%}.sbico{color:transparent;display:inline-block;height:15px;margin:0 auto;margin-top:2px;width:15px;overflow:hidden}</style><script type=\"text/javascript\">window.google = {y:{}};google.lc=[];google.li=0;var a=function(e,g,h){var k=\"\",b=new Image,l=google.lc,f=google.li;b.onerror=b.onload=b.onabort=function(){delete l[f]};l[f]=b;h||-1!=g.search(\"&ei=\")||(k=\"&ei=\"+google.kEI);e=h||\"/gen_204?atyp=i&ct=\"+e+\"&cad=\"+g+k+\"&zx=\"+(new Date).getTime();b.src=e;google.li=f+1},c=[\"google\",\"log\"],d=this;c[0]in d||!d.execScript||d.execScript(\"var \"+c[0]);for(var m;c.length&&(m=c.shift());)c.length||void 0===a?d[m]?d=d[m]:d=d[m]={}:d[m]=a;</script><script type=\"text/javascript\"></script><script>(function(){var a=function(f){for(var g=f.parentElement,d=null,e=0;e<g.childNodes.length;e++){var h=g.childNodes[e];-1<(\" \"+h.className+\" \").indexOf(\" am-dropdown-menu \")&&(d=h)}\"none\"==d.style.display?(d.style.display=\"\",google.log(\"hpam\",\"&ved=\"+f.getAttribute(\"data-ved\"))):d.style.display=\"none\"},b=[\"google\",\"sham\"],c=this;b[0]in c||!c.execScript||c.execScript(\"var \"+b[0]);for(var k;b.length&&(k=b.shift());)b.length||void 0===a?c[k]?c=c[k]:c=c[k]={}:c[k]=a;}).call(window);</script></head><body bgcolor=\"#ffffff\" marginheight=\"0\" marginwidth=\"0\" topmargin=\"0\"><div id=gbar><nobr><b class=gb1>Search</b> <a class=gb1 href=\"http://www.google.com/search?q=mining%2Bmythe&um=1&ie=UTF-8&hl=en&tbm=isch&source=og&sa=N&tab=wi\">Images</a> <a class=gb1 href=\"http://maps.google.com/maps?q=mining%2Bmythe&um=1&ie=UTF-8&hl=en&sa=N&tab=wl\">Maps</a> <a class=gb1 href=\"https://play.google.com/?q=mining%2Bmythe&um=1&ie=UTF-8&hl=en&sa=N&tab=w8\">Play</a> <a class=gb1 href=\"http://www.youtube.com/results?q=mining%2Bmythe&um=1&ie=UTF-8&sa=N&tab=w1\">YouTube</a> <a class=gb1 href=\"http://news.google.com/nwshp?hl=en&tab=wn\">News</a> <a class=gb1 href=\"https://mail.google.com/mail/?tab=wm\">Gmail</a> <a class=gb1 href=\"https://drive.google.com/?tab=wo\">Drive</a> <a class=gb1 style=\"text-decoration:none\" href=\"http://www.google.com/intl/en/options/\"><u>More</u> &raquo;</a></nobr></div><div id=guser width=100%><nobr><span id=gbn class=gbi></span><span id=gbf class=gbf></span><span id=gbe></span><a href=\"http://www.google.com/history/optout?hl=en\" class=gb4>Web History</a> | <a  href=\"/preferences?hl=en\" class=gb4>Settings</a> | <a target=_top id=gb_70 href=\"https://accounts.google.com/ServiceLogin?hl=en&continue=http://www.google.com/search%3Fq%3Dmining%252Bmythe%26start%3D30\" class=gb4>Sign in</a></nobr></div><div class=gbh style=left:0></div><div class=gbh style=right:0></div><table id=\"mn\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"position:relative\"><tr><th width=\"132\"></th><th width=\"573\"></th><th width=\"278\"></th><th></th></tr><tr><td class=\"sfbgg\" valign=\"top\"><div id=\"logocont\"><h1><a href=\"/webhp?hl=en\" style=\"background:url(/images/nav_logo176.png) no-repeat 0 -41px;height:37px;width:95px;display:block\" id=\"logo\" title=\"Go to Google Home\"></a></h1></div></td><td class=\"sfbgg\" colspan=\"2\" valign=\"top\" style=\"padding-left:0px\"><form style=\"display:block;margin:0;background:none\" action=\"/search\" id=\"tsf\" method=\"GET\" name=\"gs\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:20px;position:relative\"><tr><td><div class=\"lst-a\"><table cellpadding=\"0\" cellspacing=\"0\"><tr><td class=\"lst-td\" width=\"555\" valign=\"bottom\"><div style=\"position:relative;zoom:1\"><input class=\"lst\" value=\"mining+mythe\" title=\"Search\" autocomplete=\"off\" id=\"sbhost\" maxlength=\"2048\" name=\"q\" type=\"text\"></div></td></tr></table></div></td><td><div class=\"ds\"><div class=\"lsbb\"><button class=\"lsb\" value=\"Search\" name=\"btnG\" type=\"submit\"><span class=\"sbico\" style=\"background:url(/images/nav_logo176.png) no-repeat -36px -111px;height:14px;width:13px;display:block\"></span></button></div></div></td><td style=\"font-size:11px;padding-left:13px\"></td></tr></table></form></td><td class=\"sfbgg\">&nbsp;</td></tr><tr style=\"position:relative\"><td><div style=\"border-bottom:1px solid #ebebeb;height:59px\"></div></td><td colspan=\"2\"><div class=\"tn\"><div class=\"_UXb _Ihd _sxc _Hhd\">Web</div><div class=\"_sxc\"><a class=\"_UXb _Jhd\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnms&amp;tbm=isch&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CAUQ_AU4Hg\">Images</a></div><div class=\"_sxc\"><a class=\"_UXb _Jhd\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnms&amp;tbm=vid&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CAYQ_AU4Hg\">Videos</a></div><div class=\"_sxc\"><a class=\"_UXb _Jhd\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnms&amp;tbm=nws&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CAcQ_AU4Hg\">News</a></div><div class=\"_sxc\"><a class=\"_UXb _Jhd\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnms&amp;tbm=shop&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CAgQ_AU4Hg\">Shopping</a></div><div class=\"_sxc\"><a class=\"_UXb _Jhd\" href=\"http://maps.google.com/maps?q=mining%2Bmythe&amp;um=1&amp;ie=UTF-8&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CAkQ_AU4Hg\">Maps</a></div><div class=\"_sxc\"><a class=\"_UXb _Jhd\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnms&amp;tbm=bks&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CAoQ_AU4Hg\">Books</a></div></div><div style=\"border-bottom:1px solid #ebebeb;height:59px\"></div></td><td><div style=\"border-bottom:1px solid #ebebeb;height:59px\"></div></td></tr><tbody data-jibp=\"h\" data-jiis=\"uc\" id=\"desktop-search\"><tr><td id=\"leftnav\" valign=\"top\"><div><h2 class=\"hd\">Search Options</h2><ul class=\"med\" id=\"tbd\"><li><ul class=\"tbt\"><li class=\"tbos\" id=\"qdr_\">Any time</li><li class=\"tbou\" id=\"qdr_h\"><a class=\"q\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnt&amp;tbs=qdr:h&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CA8QpwU4Hg\">Past hour</a></li><li class=\"tbou\" id=\"qdr_d\"><a class=\"q\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnt&amp;tbs=qdr:d&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CA8QpwU4Hg\">Past 24 hours</a></li><li class=\"tbou\" id=\"qdr_w\"><a class=\"q\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnt&amp;tbs=qdr:w&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CA8QpwU4Hg\">Past week</a></li><li class=\"tbou\" id=\"qdr_m\"><a class=\"q\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnt&amp;tbs=qdr:m&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CA8QpwU4Hg\">Past month</a></li><li class=\"tbou\" id=\"qdr_y\"><a class=\"q\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnt&amp;tbs=qdr:y&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CA8QpwU4Hg\">Past year</a></li></ul></li><li><ul class=\"tbt\"><li class=\"tbos\" id=\"whv_\">All results</li><li class=\"tbou\" id=\"li_1\"><a class=\"q\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;source=lnt&amp;tbs=li:1&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CA8QpwU4Hg\">Verbatim</a></li></ul></li></ul></div></td><td valign=\"top\"><div id=\"center_col\"><div class=\"sd\" id=\"resultStats\">Page 4 of about 6,610,000 results</div><div id=\"res\"><div id=\"topstuff\"></div><div id=\"search\"><div id=\"ires\"><ol><li class=\"g\"><h3 class=\"r\"><a href=\"/images?q=mining%2Bmythe&amp;hl=en&amp;sa=X&amp;oi=image_result_group&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CBQQsAQ4Hg\">Images for <b>mining+mythe</b></a></h3><br><div><a href=\"/url?q=http://flickrhivemind.net/Tags/svay/Recent&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CBYQ9QEwADge&amp;usg=AFQjCNEfZESpa6aj2g8dKUmseCRonUETLw\"><img style=\"margin:3px 0;margin-right:6px;padding:0\" height=\"90\" src=\"http://t0.gstatic.com/images?q=tbn:ANd9GcSlyHTzwebM1syrv7HJZl5bsyzrXjtvID6q15RAfgEcIRdcdmvcC3zIU0Q\" width=\"60\" align=\"middle\" alt=\"\" border=\"0\"></a><a href=\"/url?q=http://flickrhivemind.net/Tags/svay/Recent&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CBgQ9QEwATge&amp;usg=AFQjCNEfZESpa6aj2g8dKUmseCRonUETLw\"><img style=\"margin:3px 0;margin-right:6px;padding:0\" height=\"80\" src=\"http://t3.gstatic.com/images?q=tbn:ANd9GcSd6jtoPP5Rn0qSS-toOccfupJ6W9YPdCHdIWo5Zt8yuX_E3AES2oPn_A\" width=\"115\" align=\"middle\" alt=\"\" border=\"0\"></a><a href=\"/url?q=http://www.wbrc.org.uk/worcRecd/Issue%252020/distribution_of_2_micromoths.htm&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CBoQ9QEwAjge&amp;usg=AFQjCNHlJkqYHoDOoij6KJdBbQAaHsMhEw\"><img style=\"margin:3px 0;margin-right:6px;padding:0\" height=\"86\" src=\"http://t0.gstatic.com/images?q=tbn:ANd9GcTy97h_pWtitWVuf5pXi6bQP-_uOQMn8_pn1GphriGuKPBlte7T9C4X6po\" width=\"115\" align=\"middle\" alt=\"\" border=\"0\"></a><a href=\"/url?q=http://web-japan.org/nipponia/nipponia45/fr/feature/feature10.html&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CBwQ9QEwAzge&amp;usg=AFQjCNHJpBwrfRGwRTMFcAYYCsaYfirnjw\"><img style=\"margin:3px 0;margin-right:6px;padding:0\" height=\"73\" src=\"http://t1.gstatic.com/images?q=tbn:ANd9GcQ3P0dlk9Rv_KnDROZ_bpZWgq-pu2sDKAUNeMJBHm_QU1RbfrZ4HtnZDJM\" width=\"115\" align=\"middle\" alt=\"\" border=\"0\"></a><a href=\"/url?q=http://fastlifeluxury.com/parmigiani-bugatti-mythe-430000/&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CB4Q9QEwBDge&amp;usg=AFQjCNGRBhmsmJS_lJal969s4YB5IFUepQ\"><img style=\"margin:3px 0;margin-right:6px;padding:0\" height=\"86\" src=\"http://t1.gstatic.com/images?q=tbn:ANd9GcQxXfsKGVGa2moK10PRkYtXdEjTFbX5c3udGwJTi-fCycZ1lgtrKV9Q9g\" width=\"104\" align=\"middle\" alt=\"\" border=\"0\"></a></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://www.reddit.com/r/ffxiv/comments/1ri6cu/what_is_the_best_way_to_earn_gil_end_game/&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CCAQFjAFOB4&amp;usg=AFQjCNFxd5KMS2vPZSzx45Mxig8s9U1aTQ\">What is the best way to earn gil end game? : ffxiv - Reddit</a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>www.reddit.com/r/.../what_is_the_best_way_to_earn_gil_end_game/</cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CCEQ7B0wBTge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:67OBbbNADaAJ:http://www.reddit.com/r/ffxiv/comments/1ri6cu/what_is_the_best_way_to_earn_gil_end_game/%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CCMQIDAFOB4&amp;usg=AFQjCNHtlm4-kcGbA4ftd3Saq_N49V1Scg\">Cached</a></li><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/search?ie=UTF-8&amp;q=related:www.reddit.com/r/ffxiv/comments/1ri6cu/what_is_the_best_way_to_earn_gil_end_game/+mining%2Bmythe&amp;tbo=1&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CCQQHzAFOB4\">Similar</a></li></ul></div></div></div><span class=\"st\">Nov 26, 2013 <b>...</b> I have been <b>mining</b>, im at lvl 40 miner, and i make about 100k a day, but i ... and <br>\nnow working on <b>Myth</b> i had no use for Phil tomes, and now i do!</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://tw-db.info/%3Fstrana%3Dquest%26serie%3Dqs_68&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CCcQFjAGOB4&amp;usg=AFQjCNHUOc1f_yZd-lxnzyth8uosu9sGdg\">The <b>Myth</b> - TW-DB.info</a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>tw-db.info/?strana=quest&amp;serie=qs_68</cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CCgQ7B0wBjge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:LSFMWWEscGUJ:http://tw-db.info/%3Fstrana%253Dquest%2526serie%253Dqs_68%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CCoQIDAGOB4&amp;usg=AFQjCNEYkW_4cMdFDtvX1dHP2wS5K86FYQ\">Cached</a></li><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/search?ie=UTF-8&amp;q=related:tw-db.info/%3Fstrana%3Dquest%26serie%3Dqs_68+mining%2Bmythe&amp;tbo=1&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CCsQHzAGOB4\">Similar</a></li></ul></div></div></div><span class=\"st\">Lost password? Level 90 - The <b>Myth</b> ... Coal <b>mining</b>. Reward. 1. Have equiped. 1 <br>\n&middot; 1. Other requirements. Server time: 18:00 - 20:00. Job. Iron <b>mining</b>. Reward.</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://www.independent.co.ug/the-last-word/the-last-word/7319-the-myth-of-congolese-wealth&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CC0QFjAHOB4&amp;usg=AFQjCNGZaACJvAgxzLxMo0WugkNkLNmqdg\">The <b>myth</b> of Congolese wealth - The Independent</a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>www.independent.co.ug/the.../7319-the-<b>myth</b>-of-congolese-wealth</cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CC4Q7B0wBzge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:IDo-sgGQirUJ:http://www.independent.co.ug/the-last-word/the-last-word/7319-the-myth-of-congolese-wealth%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDAQIDAHOB4&amp;usg=AFQjCNHvf1CEINo7ESj72cZK0rhVxivGsw\">Cached</a></li><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/search?ie=UTF-8&amp;q=related:www.independent.co.ug/the-last-word/the-last-word/7319-the-myth-of-congolese-wealth+mining%2Bmythe&amp;tbo=1&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDEQHzAHOB4\">Similar</a></li></ul></div></div></div><span class=\"st\">Jan 25, 2013 <b>...</b> Currently, Congo&#39;s gold and tantalite in its eastern region are <b>mined</b> by small <br>\nscale, individual or family based artisans using rudimentary tools&nbsp;...</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://www.academia.edu/3156243/Mt.Pangaion._The_War_for_its_Silver_in_Myth&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDQQFjAIOB4&amp;usg=AFQjCNHblozHvWqR-kbwAKjOiF8_0269bA\">Mt.Pangaion. The War for its Silver in <b>Myth</b> | Kostadin Rabadjiev <b>...</b></a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>www.academia.edu/.../Mt.Pangaion._The_War_for_its_Silver_in_<b>Myth</b></cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CDUQ7B0wCDge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:7PRlSUTkd18J:http://www.academia.edu/3156243/Mt.Pangaion._The_War_for_its_Silver_in_Myth%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDcQIDAIOB4&amp;usg=AFQjCNGD9SK6C8bUi9HO2qqHXxyDe0m_Kw\">Cached</a></li></ul></div></div></div><span class=\"st\">So one of the possible reasons for the conflicts were probably the silver <b>mines</b> of <br>\nPangaion and my aim is to interpret the history in myths with an idea about&nbsp;...</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://www.theguardian.com/environment/earth-insight/2014/may/22/two-thirds-write-down-us-shale-oil-gas-explodes-fracking-myth&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDkQFjAJOB4&amp;usg=AFQjCNFhW1pmJaRhu-00LnV1t8BWkrog-w\">Write-down of two-thirds of US shale oil explodes fracking <b>myth</b> <b>...</b></a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>www.theguardian.com/.../two-thirds-write-down-us-shale-oil-gas-explodes- fracking-<b>myth</b></cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CDoQ7B0wCTge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:-oJ6jKgSai0J:http://www.theguardian.com/environment/earth-insight/2014/may/22/two-thirds-write-down-us-shale-oil-gas-explodes-fracking-myth%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDwQIDAJOB4&amp;usg=AFQjCNEvj_gj5TMoUHyac7UFiZAcCiAEqg\">Cached</a></li></ul></div></div></div><span class=\"st\">May 22, 2014 <b>...</b> Write-down of two-thirds of US shale oil explodes fracking <b>myth</b> ...... <br>\nConservationists slam plans to dump <b>mining</b> waste into Norwegian fjord. 6&nbsp;...</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://www.bmlisieux.com/litterature/gambier/gambie10.htm&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CD8QFjAKOB4&amp;usg=AFQjCNG1IabLIYr-QcwnQk95hgHZIZ2Zlw\">Autour de Germinal d&#39;Emile Zola (Roman mythique, roman épique 1 <b>...</b></a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>www.bmlisieux.com/litterature/gambier/gambie10.htm</cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CEAQ7B0wCjge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:cB-MkK-5xZUJ:http://www.bmlisieux.com/litterature/gambier/gambie10.htm%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CEIQIDAKOB4&amp;usg=AFQjCNG_zcekkY5G4aNxNkkpKRdxOueLQQ\">Cached</a></li><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/search?ie=UTF-8&amp;q=related:www.bmlisieux.com/litterature/gambier/gambie10.htm+mining%2Bmythe&amp;tbo=1&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CEMQHzAKOB4\">Similar</a></li></ul></div></div></div><span class=\"st\">1) Le <b>mythe</b> culturel fondateur, je vais y insister, c&#39;est celui du Minotaure, c&#39;est-à-<br>\ndire que ... Le labyrinthe du Minotaure, c&#39;est l&#39;ensemble des pièges : la <b>mine</b> ; le&nbsp;...</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://fr.board.goodgamestudios.com/empire/showthread.php%3F91847-TUTO-Mine-de-rubis-mythe-ou-r%25C3%25A9alit%25C3%25A9&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CEUQFjALOB4&amp;usg=AFQjCNEykRje999rG561a4c0SDjenZ_R_g\">Discussion: [TUTO] <b>Mine</b> de rubis, <b>mythe</b> ou réalité? - Forum <b>...</b></a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>fr.board.goodgamestudios.com/.../showthread.php?...<b>Mine</b>-de...<b>mythe</b>...</cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CEYQ7B0wCzge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:JoiGp7OkbmAJ:http://fr.board.goodgamestudios.com/empire/showthread.php%3F91847-TUTO-Mine-de-rubis-mythe-ou-r%2525C3%2525A9alit%2525C3%2525A9%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CEgQIDALOB4&amp;usg=AFQjCNGXXWrsEn4dPF_5tix-IXpWONNp0Q\">Cached</a></li></ul></div></div></div><span class=\"st\">Bien le bonsoir, Voici un TUTO sur les légendaires <b>Mines</b> de rubis : Une <b>mine</b> de <br>\nrubis c&#39;est quoi? Une <b>mine</b> de rubis est un batiment qui vous&nbsp;...</span><br></div></li><li class=\"g\"><h3 class=\"r\"><a href=\"/url?q=http://www.lltjournal.ca/index.php/llt/article/view/2471&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CEsQFjAMOB4&amp;usg=AFQjCNFwfqWXBSzfNveuyFUtREEm6YhkRg\">Socialists and Workers: The Western Canadian Coal <b>Miners</b>, 1900-21</a></h3><div class=\"s\"><div class=\"kv\" style=\"margin-bottom:2px\"><cite>www.lltjournal.ca/index.php/llt/article/view/2471</cite><div class=\"_nBb\">&#8206;<div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CEwQ7B0wDDge\"><span class=\"_O0\"></span></div><div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\"><ul><li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:p3s87OoQBRYJ:http://www.lltjournal.ca/index.php/llt/article/view/2471%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CE4QIDAMOB4&amp;usg=AFQjCNEBnoL-G5lXGvwphUL9J1Lmggp9uw\">Cached</a></li></ul></div></div></div><span class=\"st\">This article analyzes the socialist project in the coal <b>mining</b> regions of Alberta and <br>\n... À face du <b>mythe</b> de la frontière, la plupart des socialistes ouvrière dans&nbsp;...</span><br></div></li><li class=\"g\"><table class=\"ts\"><tr><td colspan=\"2\"><h3 class=\"r\"><a href=\"/url?q=http://www.youtube.com/watch%3Fv%3D5i1lya1pUOg&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CFEQtwIwDTge&amp;usg=AFQjCNENLTQ7ggicswWpVB2n5Wco6oU6KQ\">Minecraft <b>Myth</b> Madness: Herobrine, Pig Power, Lava Dolphin, Pig <b>...</b></a></h3></td></tr><tr><td valign=\"top\" width=\"1\" style=\"padding:4px 8px 0 0\"><div class=\"th\" style=\"position:relative;overflow:hidden;width:116px;height:65px\"><a href=\"/url?q=http://www.youtube.com/watch%3Fv%3D5i1lya1pUOg&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CFIQuAIwDTgeUAE&amp;usg=AFQjCNENLTQ7ggicswWpVB2n5Wco6oU6KQ\"><div style=\"margin-top:-11px\"><img src=\"http://img.youtube.com/vi/5i1lya1pUOg/default.jpg?h=90&amp;w=120&amp;sigh=__iZk2OGJRwoFMN1WIIptcvalZUUQ=\" width=\"116\" align=\"middle\" alt=\"\" border=\"0\"></div><div style=\"position:absolute;bottom:0;right:0;font-size:11px;color:#fff;background-color:#222;padding:1px 3px;text-decoration:none;font-weight:bold;text-align:right\">&#9658; 6:54</div></a></div></td><td valign=\"top\" style=\"padding-top:1px\"><cite class=\"kv\">www.youtube.com/watch?v=5i1lya1pUOg</cite><span class=\"st\"><span class=\"f\"><span class=\"nobr\">Aug 25, 2012</span> - <span class=\"nobr\">7 min</span> - <span class=\"nobr\">Uploaded by Jolly ol Brits play Minecraft</span></span><br>Try milking a baby cow my <b>mine</b> craft name is £973+= ... <b>myth</b> to test: if you spam  shift, will it <b>...</b></span></td></tr></table><div style=\"margin:8px 0 5px\"><a class=\"fl\" href=\"#\"></a></div></li></ol></div></div></div></div><div id=\"foot\"><table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" id=\"nav\"><tr valign=\"top\"><td align=\"left\" class=\"b\"><a href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=20&amp;sa=N\"><span class=\"csb\" style=\"background-position:0 0;float:right;width:53px\"></span><span style=\"display:block;margin-right:35px;clear:right\">Previous</span></a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=0&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>1</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=10&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>2</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=20&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>3</a></td><td><span class=\"csb\" style=\"background-position:-53px 0;width:20px\"></span><b>4</b></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=40&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>5</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=50&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>6</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=60&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>7</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=70&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>8</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=80&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>9</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=90&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>10</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=100&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>11</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=110&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>12</a></td><td><a class=\"fl\" href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=120&amp;sa=N\"><span class=\"csb\" style=\"background-position:-74px 0;width:20px\"></span>13</a></td><td class=\"b\" style=\"text-align:left\"><a href=\"/search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;start=40&amp;sa=N\" style=\"text-align:left\"><span class=\"csb\" style=\"background-position:-96px 0;width:71px\"></span><span style=\"display:block;margin-left:53px\">Next</span></a></td></tr></table><p class=\"_cD\" id=\"bfl\" style=\"margin:19px 0 0;text-align:center\"><a href=\"/advanced_search?q=mining%2Bmythe&amp;ie=UTF-8&amp;prmd=ivns\">Advanced search</a><a href=\"/support/websearch/bin/answer.py?answer=134479&amp;hl=en\">Search Help</a> <a href=\"/tools/feedback/survey/html?productId=196&amp;query=mining%2Bmythe&amp;hl=en\">Send feedback</a></p><div class=\"_cD\" id=\"fll\" style=\"margin:19px auto 19px auto;text-align:center\"><a href=\"/\">Google&nbsp;Home</a> <a href=\"/intl/en/ads\">Advertising&nbsp;Programs</a> <a href=\"/services\">Business Solutions</a> <a href=\"/intl/en/policies/\">Privacy & Terms</a> <a href=\"/intl/en/about.html\">About Google</a></div></div></td><td valign=\"top\"></td></tr></tbody></table><script type=\"text/javascript\">(function(){var eventid='88ZvVJj2Io_aiAK434HoDw';google.kEI = eventid;})();</script><script src=\"/xjs/_/js/k=xjs.hp.en_US.YU1Y-X5EHRU.O/m=sb_he,pcc/rt=j/d=1/t=zcms/rs=ACT90oF1XCz-9XT7lCNfAUenHvDScFfezw\"></script><script type=\"text/javascript\">google.ac&&google.ac.c({\"agen\":true,\"cgen\":true,\"client\":\"heirloom-serp\",\"dh\":true,\"ds\":\"\",\"eqch\":true,\"fl\":true,\"host\":\"google.com\",\"jam\":0,\"jsonp\":true,\"msgs\":{\"cibl\":\"Clear Search\",\"dym\":\"Did you mean:\",\"lcky\":\"I\\u0026#39;m Feeling Lucky\",\"lml\":\"Learn more\",\"oskt\":\"Input tools\",\"psrc\":\"This search was removed from your \\u003Ca href=\\\"/history\\\"\\u003EWeb History\\u003C/a\\u003E\",\"psrl\":\"Remove\",\"sbit\":\"Search by image\",\"srch\":\"Google Search\"},\"ovr\":{},\"pq\":\"mining+mythe\",\"qcpw\":false,\"refoq\":true,\"scd\":10,\"sce\":5,\"stok\":\"egFRNsbH8c0rnW5T0janT1-s6iE\"})</script></body></html>")
    (def parsed-doc (parse-html query-result-html))
    (def results-block (select-one parsed-doc "#res"))
    (def results-rcs (select results-block ".g"))
    
    (def result-html "<h3 class=\"r\"><a href=\"/url?q=http://www.independent.co.ug/the-last-word/the-last-word/7319-the-myth-of-congolese-wealth&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CC0QFjAHOB4&amp;usg=AFQjCNGZaACJvAgxzLxMo0WugkNkLNmqdg\">The <b>myth</b> of Congolese wealth - The Independent</a></h3>\n<div class=\"s\">\n <div class=\"kv\" style=\"margin-bottom:2px\">\n  <cite>www.independent.co.ug/the.../7319-the-<b>myth</b>-of-congolese-wealth</cite>\n  <div class=\"_nBb\">\n   ‎\n   <div style=\"display:inline\" onclick=\"google.sham(this);\" aria-expanded=\"false\" aria-haspopup=\"true\" tabindex=\"0\" data-ved=\"0CC4Q7B0wBzge\">\n    <span class=\"_O0\"></span>\n   </div>\n   <div style=\"display:none\" class=\"am-dropdown-menu\" role=\"menu\" tabindex=\"-1\">\n    <ul>\n     <li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/url?q=http://webcache.googleusercontent.com/search%3Fq%3Dcache:IDo-sgGQirUJ:http://www.independent.co.ug/the-last-word/the-last-word/7319-the-myth-of-congolese-wealth%252Bmining%252Bmythe%26hl%3Den%26%26ct%3Dclnk&amp;sa=U&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDAQIDAHOB4&amp;usg=AFQjCNHvf1CEINo7ESj72cZK0rhVxivGsw\">Cached</a></li>\n     <li class=\"_Ykb\"><a class=\"_Zkb\" href=\"/search?ie=UTF-8&amp;q=related:www.independent.co.ug/the-last-word/the-last-word/7319-the-myth-of-congolese-wealth+mining%2Bmythe&amp;tbo=1&amp;sa=X&amp;ei=88ZvVJj2Io_aiAK434HoDw&amp;ved=0CDEQHzAHOB4\">Similar</a></li>\n    </ul>\n   </div>\n  </div>\n </div>\n <span class=\"st\">Jan 25, 2013 <b>...</b> Currently, Congo's gold and tantalite in its eastern region are <b>mined</b> by small <br /> scale, individual or family based artisans using rudimentary tools&nbsp;...</span>\n <br />\n</div>")
    (def a-rc (nth results-rcs 3))
    (def rc-h3 (select-one a-rc "h3.r"))
    (def heading-text (text rc-h3))
    (def r-link (get (->> (select-one a-rc "a") (attr "href") java.net.URI. .getQuery codec/form-decode) "q"))
    (def meta-description (-> a-rc (select-one ".st") text))
    ))

(def results-per-query  "How many results get crawled for a query." 500)

(defn- result-pages "Creates a lazy seq of the pages to fetch for a given query (given that each page contains 10 results)"
  [^String query]
  (let [query_terms (s/split query #"\W")
        search {:query_terms query_terms
                :query query}]
    (->> (range results-per-query) (take-nth 10)
      (map #(assoc search :start_index %)))
    ))

(defn- result-page-request-opts [{:keys [query_terms start_index] :as page}]
  {:query-params {"q" (s/join "+" query_terms)
                  "start" (str start_index)
                  "lr" "lang_en"}})

(defn fetch-result-page "Fetches a results page of the specified search at the specified page index"
  [{:keys [query_terms start_index] :as page}]
  (assoc page 
         :query_result_html (-> @(http/get "http://www.google.com/search" (result-page-request-opts page)) ;; getting the results in English only
                              :body)
         ))
(defn fetch-result-page-a "Asynchronous version of fetch-result-page"
  [page ch]
  (http/get "http://www.google.com/search" (result-page-request-opts page) 
            (fn [resp] 
              (>!!-and-close! 
                ch (assoc page :query_result_html (resp :body)) 
                ))
            ))

(defn- is-images-result? "Detects if a result block is an 'images' result" 
  [{:keys [result-elem]}]
  (-> result-elem (select "img") count (> 1)))

(defn get-search-results "Breaks a results page into the DOM elements that are individual results" 
  [{:keys [query_result_html start_index], :as page}]
  (let [parsed-doc (parse-html query_result_html)
        results-block (select-one parsed-doc "#res")
        results-rcs (select results-block ".g")]
    (->> results-rcs 
      (map (fn [idx r] 
             (-> page 
               (assoc :rank (+ start_index idx),:result-elem r,:result_html (html r))
               (dissoc :start_index :query_result_html))
             ) (range))
      (remove is-images-result?)
    )))

(defn crawl-search-result "Performs scraping of a search result element. Extracts heading text, link URL and meta-description."
  [{:keys [result-elem] :as r}]
  (let [rc-h3 (select-one result-elem "h3.r")
        heading-text (text rc-h3)
        r-link (get (->> (select-one result-elem "a") (attr "href") java.net.URI. .getQuery codec/form-decode) "q")
        meta-description (-> result-elem (select-one ".st") text)]
    (-> r 
      (assoc :heading heading-text
             :link r-link
             :meta_description meta-description)
      (dissoc :result-elem)
      )))

;; ----------------------------------------------------------------
;; Target website fetching
;; ----------------------------------------------------------------

(defn- add-website "From a result map and the HTTP response of fetching the result's website, assoc's the website HTML to the result it the response is well-formed; otherwise returns the raw result." 
  [r,{{:keys [content-type]} :headers, body :body :as resp}]
  (cond-> r 
          (and content-type (.startsWith content-type "text/html") (string? body)) ;; tests that response is html
          (assoc :website_html (->> body minify-html))
          ))

(defn fetch-website "Fetches the HTML of the website of the result." 
  [{:keys [link] :as r}]
  (cond-> r link (add-website @(http/get link))))

(defn fetch-website-a "Fetches the HTML of the website of the result; meant to be called with pipeline-async" 
  [{:keys [link] :as r} ch]
  (http/get link (fn [resp] 
                   (>!!-and-close! ch (add-website r resp))
                   )))

(defn- add-rank "Adds a :rank property to a sequence of maps." 
  [s] (map (fn [i item] (assoc item :rank i)) (range) s))

;; ----------------------------------------------------------------
;; Tokenization
;; ----------------------------------------------------------------

(comment ;; tokenization sandbox
  (do 
    (def wikipedia-text (-> @(http/get "http://en.wikipedia.org/wiki/Mining") :body  parse-html text))
    (def wikipedia-tokens (->> wikipedia-text tokenize-simple))
    (def wikipedia-bag (reduce-to-bag-of-words wikipedia-tokens))
    (def wikipedia-seq-bag (to-seq-BoW wikipedia-bag))
    ))

(defn- only-alpha? "Checks if a token contains only alphabet characters" 
  [{:keys [lemma]}] (re-matches #"[a-z]*" lemma))
(defn- too-short? 
  [{:keys [lemma]}] (-> (count lemma) (< 3)))

(defn- tokenize-simple "Tokenizes a text in a way that keeps (mostly) only words tokens, with lowercased lemmas."
  [text]
  (->> text nlpu/tokenize 
    (remove nlpu/non-word-token?) ;; removing symbols etc.
    (map nlpu/lowercase-lemma) ;; putting all lemmas to lower-case
    (filter only-alpha?)
    (remove nlpu/stopword-token?) ;; removing stopwords ("the", "a" "be", ...)
    (remove too-short?)
    ))

;; weights of the tokens found in the various fields.
(def ^:dynamic weight-heading 1)
(def ^:dynamic weight-meta_description 1)
(def ^:dynamic weight-website-content 1)

(defn- reduce-to-bag-of-words "Reduces a sequence of token-maps that have a :lemma and an (optional) :weight properties into a Bag-Of-Words representation." 
  [tokens]
  (->> tokens
    (reduce (fn [res {:keys [lemma weight]}] (assoc! res lemma (+ (get res lemma 0) (or weight 1.0)))) 
            (transient {}))
    persistent!)
  )

(defn- normalize-BoW [bag]
  (let [total (->> bag vals (reduce + 0) (* 1.0))]
    (if (> total 0)
      (persistent!
        (reduce (fn [b k] (assoc! b k (/ (b k) total))) 
          (transient bag) (keys bag)))
      bag)))

(defn- to-seq-BoW "From a map Bag-of-Words representation, creates a sequential representation, 
that is a seq of maps with :lemma and :weight properties."
  [bag]
  (map (fn [[word weight]] {:word word :weight weight}) bag))

(defn tokenize-result 
  [{:keys [heading meta_description website_html] :as r}]
  (let [weighted-tokens 
        (cond-> 
          ()
          heading (concat (->> heading tokenize-simple (map #(assoc % :weight weight-heading))))
          meta_description (concat (->> meta_description tokenize-simple (map #(assoc % :weight weight-meta_description))))
          website_html (concat (->> website_html parse-html text tokenize-simple (map #(assoc % :weight weight-website-content))))
          )]
    (assoc r :tokens_bag (-> weighted-tokens reduce-to-bag-of-words to-seq-BoW)))
  )

;; ----------------------------------------------------------------
;; Total sequence
;; ----------------------------------------------------------------

(defn crawled-results-seq "Returns the crawled results corresponding to the query as a lazy sequence"
  [^String query]
  (->> (result-pages query)
    (failsafe-pmap fetch-result-page) 
    (mapcat get-search-results)
    (failsafe-map crawl-search-result)
    
    ;(map-pipeline-async fetch-website-a)
    
    ;(pmap tokenize-result)
    
    ;time
    ))

;; ----------------------------------------------------------------
;; MongoDB
;; ----------------------------------------------------------------
(def mongo-uri "mongodb://localhost:27017/sviepbd")
(defn connect-mongo! [] (mg/connect-via-uri! mongo-uri))
(comment (connect-mongo!))

(def results-coll "MongoDB collection of 9000 results - some in French - with approximate ranking." 
  "results")
(defn get-results [] (mc/find-maps results-coll))
(def save-result! #(mc/save results-coll %))

(def dissoc-_id #(dissoc % :_id))

(def complete-results-coll "MongoDB collection of 421 results with their website's HTML fetched"
  "results_complete")
(defn get-complete-results [] (mc/find-maps complete-results-coll))
(def save-complete-result! #(mc/save complete-results-coll %))

(comment
  ;; saving all results to mongo
  (doseq [r (crawled-results-seq "mining")]
    (save-result! r))
  )

;; ----------------------------------------------------------------
;; SQLite
;; ----------------------------------------------------------------
(def sqlite-connection 
  (db/sqlite3 {:db (-> "data.db" io/resource .getPath)})
  ;(db/h2 {:db (-> "datah2.db" io/resource .getPath)})
  )
(db/defdb sqlite-db sqlite-connection) ;; connects to SQLite database which file is resources/data.db (unversioned)
(def sqlite-pool @(:pool sqlite-db))

(def results-table "results")
(def words-table "words")
(def bow-table "bags_of_words")

(defn create-results-table! []
  (sql/db-do-commands sqlite-pool 
                      (sql/create-table-ddl
                        :results
                        [:id :integer "PRIMARY KEY" "AUTOINCREMENT"]
                        [:heading :varchar]
                        [:meta_description :varchar]
                        [:rank :integer]
                        [:query :varchar]
                        )))
(defn drop-results-table! [] 
  (sql/db-do-commands sqlite-pool (sql/drop-table-ddl :results)))

(defn create-words-table! []
  (sql/db-do-commands sqlite-pool 
                      (sql/create-table-ddl
                        :words
                        [:id :integer]
                        [:word :varchar "UNIQUE"]
                        )))
(defn drop-words-table! [] 
  (sql/db-do-commands sqlite-pool (sql/drop-table-ddl :words)))

(declare result-ent word bags_of_words)

(k/defentity result-ent
  (k/table results-table)
  (k/prepare (comp #(select-keys % [:heading :meta_description :rank :query])
                   ))
  (k/many-to-many word bow-table)
  )
(def save-result-to-sqlite! #(k/insert result-ent (k/values %)))

(k/defentity word
  (k/table words-table)
  (k/prepare (comp #(select-keys % [:word :id])
                   ))
  (k/many-to-many result-ent bow-table)
  )
(defn save-words! [{:keys [word]} & [con]]
  (sql/execute! (or con sqlite-pool) ["INSERT OR IGNORE INTO \"words\" (\"word\") VALUES (?);" word]))

;; a table to use as a sparse matrix reprensentation.
(defn create-bow-table! []
  (sql/db-do-commands sqlite-pool
                      (sql/create-table-ddl
                        bow-table
                        [:result_id :integer]
                        [:word :varchar]
                        [:weight :double]
                        )))
(defn drop-bow-table! [] (sql/db-do-commands sqlite-pool (sql/drop-table-ddl bow-table)))
(k/defentity bags_of_words
  (k/table bow-table)
  (k/prepare (comp #(select-keys [:word :weight]))
             )
  )

(defn export-matrices! [{:keys [matrix-file, dictionary-file 
                                bow-table-name, words-table-name] 
                         :or {matrix-file "words_matrix.csv", dictionary-file "dictionary.csv"
                              bow-table-name bow-table, words-table-name words-table}}]
  ;; printing bags-of-words table
  (log/info (str "Exporting the bags of words as sparse matrix into " 
                 (.getAbsolutePath (io/as-file matrix-file)) 
                 "..."))
  (with-open [wtr (io/writer matrix-file :append false)] 
    (csv/write-csv wtr 
      (rest (sql/query sqlite-pool
                       [(format "
SELECT result_id,id,weight
FROM %s AS bows
JOIN %s AS ws
WHERE bows.word=ws.word;" bow-table-name words-table-name) 
                        ]
                       :as-arrays? true
                 ))
      :separator \space
      ))
  (log/info (str "Exported a matrix of "
                 (-> (sql/query sqlite-pool 
                                [(format "SELECT COUNT(DISTINCT result_id) AS count FROM %s ;" bow-table-name)]) first :count) " results"
                 " x "
                 (-> (sql/query sqlite-pool 
                                [(format "SELECT COUNT(*) AS count FROM %s ;" words-table-name)]) first :count) " words"
                 ))
  
  ;; printing words table
  (log/info (str "Exporting the words table into " 
                 (.getAbsolutePath (io/as-file dictionary-file)) 
                 "..."))
  (with-open [wtr (io/writer dictionary-file :append false)] 
    (csv/write-csv wtr 
                   (rest (sql/query sqlite-pool "SELECT id,word FROM words"
                                    :as-arrays? true
                                    ))
                   :separator \space
                   ))
  
  )

(comment ;; how to get sparse matrix representations
  ;; clean SQL database
  (do 
    (drop-words-table!)
    (drop-bow-table!)
  
    (create-words-table!)
    (create-bow-table!))
  
  ;; start with putting all the words in the database
  (sql/with-db-transaction [con sqlite-connection]
    (->> (get-complete-results)
      (progress-logging-seq 10)
      (mapcat :tokens_bag)
      (map #(save-words! % con))
      dorun
      time
     ))
  ;; adding word ids
  (->> (k/select word)
    (map (fn [i w] (assoc w :id i)) (range))
    (map #(k/update word (k/set-fields {:id (% :id)}) (k/where {:word (% :word)})))
    (progress-logging-seq 500)
    dorun time
    )
  
  ;; storing tokenization in bags_of_words table
  (sql/with-db-transaction [con sqlite-connection]
    (->> (get-complete-results)
      (progress-logging-seq 10)
      (map (fn [i r] (assoc r :result_id i)) (range))
      (mapcat (fn [{:keys [tokens_bag result_id]}] 
                (->> tokens_bag (map #(assoc % :result_id result_id)))
                ))
      (map #(select-keys % [:word :weight :result_id]))
      (map #(sql/insert! con bow-table %))
      dorun
      time
     ))
  
  ;;exporting 
  (export-matrices! {:matrix-file "unversioned/sparse_matrices/few-results_matrix.csv"
                     :dictionary-file "unversioned/sparse_matrices/few-results_words.csv"})
  ;; - or -
  (export-matrices! {:matrix-file "unversioned/sparse_matrices/many-results_matrix.csv"
                     :dictionary-file "unversioned/sparse_matrices/many-results_words.csv"})
  )

;; ----------------------------------------------------------------
;; Scripts
;; ----------------------------------------------------------------

(defn perform-scraping! []
  (log/info "Connecting to MongoDB...")
  (connect-mongo!)
  (log/info "Starting to scrape...")
  (->> (crawled-results-seq "mining")
    (failsafe-map save-result!)
    progress-logging-seq dorun time)
  (log/info "done scraping.")
  )
