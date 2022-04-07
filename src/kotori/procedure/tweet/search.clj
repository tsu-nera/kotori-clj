(ns kotori.procedure.tweet.search)

;; 引用RT特定方法は非公開アカウントにて
;; 'twitter.com/{screen_name}/status' -from:@{screen_name}'
;; url:{screen_name} -from:@{screen_name}
;;
;; これでも見えない場合はstreaming APIを利用する.
