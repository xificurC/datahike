(ns example.core
  (:require [datahike.api :as d]))

; first define data model
(def schema [{:db/ident :contributor/name
              :db/valueType :db.type/string
              :db/unique :db.unique/identity
              :db/index true
              :db/cardinality :db.cardinality/one
              :db/doc "a contributor's name"}
             {:db/ident :contributor/email
              :db/valueType :db.type/string
              :db/cardinality :db.cardinality/many
              :db/doc "a contributor's email"}
             {:db/ident :repository/name
              :db/valueType :db.type/string
              :db/unique :db.unique/identity
              :db/index true
              :db/cardinality :db.cardinality/one
              :db/doc "a repository's name"}
             {:db/ident :repository/contributors
              :db/valueType :db.type/ref
              :db/cardinality :db.cardinality/many
              :db/doc "the repository's contributors"}
             {:db/ident :repository/public
              :db/valueType :db.type/boolean
              :db/cardinality :db.cardinality/one
              :db/doc "toggle whether the repository is public"}
             {:db/ident :repository/tags
              :db/valueType :db.type/keyword
              :db/cardinality :db.cardinality/many
              :db/doc "the repository's tags"}])

;; define uri
(def uri "datahike:mem://schema-intro")

;; create the in-memory database
(d/create-database uri :initial-tx schema)

;; connect to it
(def conn (d/connect uri))

;; let's insert our first user
(d/transact! conn [{:contributor/name "alice" :contributor/email "alice@exam.ple"}])

;; let's find him with a query
(def find-name-email '[:find ?e ?n ?em :where [?e :contributor/name ?n] [?e :contributor/email ?em]])

(d/q find-name-email (d/db conn))

;; let's find him directly, as contributior/name is a unique, indexed identity
(d/pull (d/db conn) '[*] [:contributor/name "alice"])

;; add a second email
(d/transact! conn [{:db/id 7 :contributor/email "alice@test.test"}])

;; let's see both emails
(d/q find-name-email (d/db conn))

;; try to add something completely out of schema
(d/transact! conn [{:something "different"}])
;; => error occurs

;; try add wrong contributor values
(d/transact! conn [{:contributor/email :alice}])

;; add another contributor
(d/transact! conn [{:contributor/name "bob" :contributor/email "bob@ac.me"}])

(d/q find-name-email (d/db conn))

(d/pull (d/db conn) '[*] [:contributor/name "bob"])

;; change bobs name to bobby
(d/transact! conn [{:db/id 8 :contributor/name "bobby"}])

;; check it
(d/q find-name-email (d/db conn))

(d/pull (d/db conn) '[*] [:contributor/name "bobby"])

;; bob is not related anymore as index
(d/pull (d/db conn) '[*] [:contributor/name "bob"])

;; create a repository, with refs from uniques
(d/transact! conn [{:repository/name "top secret"
                    :repository/public false
                    :repository/contributors [[:contributor/name "bobby"] [:contributor/name "alice"]]
                    :repository/tags :clojure}])

;; let's search with pull inside the query
(def find-repositories '[:find (pull ?e [*]) :where [?e :repository/name ?n]])

;; looks good
(d/q find-repositories (d/db conn))

;; let's go further and fetch the related contributor data as well
(def find-repositories-with-contributors '[:find (pull ?e [* {:repository/contributors [*]}]) :where [?e :repository/name ?n]])

(d/q find-repositories-with-contributors (d/db conn))

;; the schema is part of the index, so we can query them too.
;; Let's find all attribute names and their description.
(d/q '[:find ?a ?d :where [?e :db/ident ?a] [?e :db/doc ?d]] (d/db conn))



;; TODO: add schema-on-read example
