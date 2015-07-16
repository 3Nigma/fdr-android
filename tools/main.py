__author__ = 'vic'

import glob
import os
import urllib2
import sqlite3
import json
import shutil
from xml.etree import ElementTree as et

jPodcastsSaveLocation = "./jFDRpodcasts"
fdrDatabaseName = "fdr.db"

def cleanEnvironment():
    print "Cleaning up the environment ..."
    try:
       shutil.rmtree(jPodcastsSaveLocation)
    except OSError:
       pass
    try:
        os.remove(fdrDatabaseName)
    except OSError:
        pass

def downloadFDRPodcasts(saveLocation, feedSlug):
    url = "http://media.freedomainradio.com/api/?method=ListPodcastFeedItems&feedSlug=" + feedSlug
    file_name = saveLocation + "/" + feedSlug + ".json"

    u = urllib2.urlopen(url)
    meta = u.info()

    clHeader = meta.getheaders("Content-Length")
    if clHeader:
        file_size = int(clHeader[0])
        print "Downloading %s wit %s bytes ..." % (file_name, file_size)
    else:
        print "Downloading %s of unknown size ..." % file_name

    f = open(file_name, 'wb')
    file_size_dl = 0
    block_sz = 8192
    while True:
        buffer = u.read(block_sz)
        if not buffer:
            break

        file_size_dl += len(buffer)
        f.write(buffer)
    f.close()

    # Check to see if what we downloaded was a slug and not a non-existing entity
    slugexists = True
    with open(file_name) as data_file:
        data = json.load(data_file)
        if "error" in data and data["error"]["errorName"] == "PodcastFeedNotFound":
            slugexists = False
    if not slugexists:
        os.remove(file_name)

    return slugexists

def compileDatabase(dbName, podcastsLocation):
    dbConn = sqlite3.connect(dbName)

    dbConn.execute('''CREATE TABLE Podcasts
       (_id INTEGER PRIMARY KEY  AUTOINCREMENT,
        podcastId      CHAR(24),
        title          TEXT     NOT NULL,
        description    TEXT     NOT NULL,
        authorId       INT      NOT NULL,
        date           INT      NOT NULL,
        length         INT      NOT NULL
        );''')
    dbConn.execute('''CREATE TABLE URLs
       (_id INTEGER PRIMARY KEY  AUTOINCREMENT,
        podcastId      INT      NOT NULL,
        typeId         INT      NOT NULL,
        value          TEXT     NOT NULL
        );''')
    dbConn.execute('''CREATE TABLE URLTypes
       (_id INTEGER PRIMARY KEY  AUTOINCREMENT,
        name          TEXT      NOT NULL
        );''')
    dbConn.execute('''CREATE TABLE Tags
       (_id INTEGER PRIMARY KEY  AUTOINCREMENT,
        tagId         TEXT      NOT NULL,
        name          TEXT      NOT NULL
        );''')
    dbConn.execute('''CREATE TABLE pt_links
       (_id INTEGER PRIMARY KEY  AUTOINCREMENT,
        tagId         INT       NOT NULL,
        podcastId     INT       NOT NULL
        );''')
    dbConn.execute('''CREATE TABLE Authors
       (_id INTEGER PRIMARY KEY  AUTOINCREMENT,
        name         TEXT       NOT NULL,
        email        TEXT       NOT NULL,
        website      TEXT       NOT NULL
        );''')

    # make sure we process the podcasts in the right order
    fdrSlugFiles = filter(os.path.isfile, glob.glob(podcastsLocation + "/*"))
    fdrSlugFiles.sort(key=lambda x: os.path.getmtime(x))
    for jPodcastSlugFileName in fdrSlugFiles:
        print "Processing %s ..." % jPodcastSlugFileName
        with open(jPodcastSlugFileName) as podcastSlugFile:
            jPodcastData = json.load(podcastSlugFile)
            for podcastEntry in jPodcastData["result"]["podcasts"]:
                # podcastId = podcastEntry["num"] -- this cannot be used because it's not unique
                podcastHashId = podcastEntry["podcastID"]
                podcastTitle = podcastEntry["title"]
                podcastDescription = podcastEntry["description"]
                podcastDate = podcastEntry["date"] * 1000  # the original date is provided in seconds while we need ms
                podcastLength = podcastEntry["length"]

                # process the URLs
                podcastURLs = []
                for urlObj in podcastEntry["urls"]:
                    # we are only interested in urls that have values
                    if urlObj["value"]:
                        # lock onto the type
                        urlTypeId = -1
                        urlCursor = dbConn.execute("SELECT _id from URLTypes where name = ?",  (urlObj["urlType"],))
                        urlCursorRows = urlCursor.fetchall()
                        if len(urlCursorRows) == 0:
                            # this url type is not present in the database. Add it and continue
                            urlTypeInsertCursor = dbConn.execute('''INSERT INTO URLTypes (name) VALUES (:name)''',
                                                                 {"name" : urlObj["urlType"]})
                            urlTypeId = urlTypeInsertCursor.lastrowid
                        else:
                            urlTypeId = urlCursorRows[0][0]

                        # we postpone the insertion of the URLs inside the database since we don't have the
                        # podcast Id available at this point
                        podcastURLs.append((urlTypeId, urlObj["value"]));

                # process the tags
                podcastTagIds = []
                for tagObj in podcastEntry["tags"]:
                    # lock onto the name
                    tagId = -1
                    tagCursor = dbConn.execute("SELECT _id from Tags where name = ?",  (tagObj["tagName"],))
                    tagCursorRows = tagCursor.fetchall()
                    if len(tagCursorRows) == 0:
                        # tag does not exist so we first have to insert it in the database and then continue
                        tagInsertCursor = dbConn.execute('''INSERT INTO Tags (tagId, name) VALUES (:tagId, :name)''',
                                                         {"tagId" : tagObj["tagID"], "name" : tagObj["tagName"]})
                        tagId = tagInsertCursor.lastrowid
                    else:
                        tagId = tagCursorRows[0][0]

                    # we postpone associating the tag with the current podcast via entries in the pt_links table until
                    # we have it stored in the database, but we store the tag references for that
                    podcastTagIds.append(tagId)

                # process the author
                podcastAuthorId = -1
                podcastAuthorObj = podcastEntry["author"]
                authorCursor = dbConn.execute("SELECT _id from Authors where name = ?",  (podcastAuthorObj["name"],))
                authorCursorRows = authorCursor.fetchall()
                if len(authorCursorRows) == 0:
                    # author is not present in the database. Add it and continue
                    authorInsertCursor = dbConn.execute('''INSERT INTO Authors (name,email,website)
                                                           VALUES (:name, :email, :website)''', podcastAuthorObj)
                    podcastAuthorId = authorInsertCursor.lastrowid
                else:
                    podcastAuthorId = authorCursorRows[0][0]

                # We have everything we need. Insert the podcast
                podcastCursor = dbConn.execute('''INSERT INTO Podcasts (podcastId, title, description, authorId, date, length)
                                                  VALUES (:podcastId, :title, :description, :authorId, :date, :length)''',
                               {"podcastId": podcastHashId, "title": podcastTitle, "description": podcastDescription,
                                "authorId": podcastAuthorId, "date": podcastDate, "length": podcastLength})
                podcastId = podcastCursor.lastrowid
                # insert the urls
                for urlData in podcastURLs:
                    dbConn.execute('''INSERT INTO URLs (podcastId, typeId, value) VALUES (:podcastId, :type, :value)''',
                                      {"podcastId" : podcastId, "type" : urlData[0], "value" : urlData[1]})

                # associate the tags
                for tagId in podcastTagIds:
                    dbConn.execute('''INSERT INTO pt_links (tagId, podcastId) VALUES (:tag, :podcast)''',
                                   {"tag" : tagId, "podcast": podcastId})

    dbConn.commit()
    dbConn.close()

def incrementAndroidDbVersion():
    print "Incrementing the Android database version number ..."
    xmlStringHolder = "../app/src/main/res/values/strings.xml"
    tree = et.parse(xmlStringHolder)
    dbVersNode = tree.find("string[@name='db_version']")
    oldDbVersion =  dbVersNode.text
    dbVersNode.text = "%d" % (int(oldDbVersion) + 1)
    tree.write(xmlStringHolder)

if __name__ == "__main__":
    cleanEnvironment()

    # Setup the environment before we start
    print "Setting everything up before starting ..."
    os.makedirs(jPodcastsSaveLocation)
    podcasStartIndex = 0
    podcastEndIndex = 250
    fdrSlug = "podcasts-1-250"

    # download all the FDR podcasts
    while downloadFDRPodcasts(jPodcastsSaveLocation, "podcasts-%d-%d" % (podcasStartIndex, podcastEndIndex)):
        podcasStartIndex += 251 if podcasStartIndex == 0 else 250
        podcastEndIndex += 250

    # compile them into the sqlite database
    # we assume that all the slugs present in 'jPodcastsSaveLocation' are clean and valid
    compileDatabase(fdrDatabaseName, jPodcastsSaveLocation)

    # dump the SQL structure and make it available to the android app
    print "Generating the SQL syntax structures ..."
    os.system('sqlite3 fdr.db .dump > ../app/src/main/res/raw/fdr.sql')

    incrementAndroidDbVersion()
    cleanEnvironment()
