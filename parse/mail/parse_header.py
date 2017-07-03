import time

import avro
from avro.datafile import DataFileWriter, DataFileReader
from avro.io import DatumWriter, DatumReader
from dateutil.parser import parse as parse_date
from email.parser import HeaderParser
import re

from os import listdir

from os.path import join, isfile, isdir


def parse_mail(files):
    parser = HeaderParser()
    emails = []

    for file in files:
        with open(file, "r") as f:
            msg = parser.parse(f)

        email = dict()
        email['from'] = msg['X-From']       # could be '', but not null
        to = [x.strip() for x in msg['X-To'].split(',') if x != '']
        cc = [x.strip() for x in msg['X-cc'].split(',') if x != '']
        bcc = [x.strip() for x in msg['X-bcc'].split(',') if x != '']
        email['to'] = to + cc + bcc         # could be an empty list, but not null
        email['to_count'] = len(email['to'])        # num of recipients in the distribution list
        subject = re.sub(r'\n|\r|\t|\s+', ' ', msg['Subject']).rstrip()
        subject = re.sub(r'^\s+', '', subject)
        # remove the fw: or re: so we can do groupBy(subject) to aggregate by subject
        processed_subject = re.sub(r'^fw:\s*|^re:\s*', '', subject, flags=re.IGNORECASE)

        email['subject'] = subject
        email['proc_subj'] = processed_subject

        # todo - the parsing did not get the timezone
        d = parse_date(msg['Date'])
        email['date'] = int(time.mktime(d.timetuple()))     # convert to unix time
        f = file.split('/')
        email['file'] = '/'.join(str(x) for x in f[-2:])    # '1/12332.txt'

        emails.append(email)

    return emails


def get_files(dir_prefix):
    # todo - not flexible enough to read arbitrary directory structure
    dirs = [d for d in listdir(dir_prefix) if isdir(join(dir_prefix, d))]  # first level subdir
    files = []
    for d in dirs:
        _d = join(dir_prefix, d)
        _f = [join(_d, f) for f in listdir(_d) if isfile(join(_d, f))]
        files.extend(_f)

    files = [f for f in files if f.endswith('.txt')]  # filter out the *.cats file
    return files


def write_to_avro(schema_file, data, outfile="/tmp/emails.avro"):
    schema = avro.schema.parse(open(schema_file, "rb").read())
    writer = DataFileWriter(open(outfile, "wb"), DatumWriter(), schema)
    for email in data:
        writer.append(email)
    writer.close()


def read_from_avro(avro_file="/tmp/emails.avro"):
    reader = DataFileReader(open(avro_file, 'rb'), DatumReader())
    for x in reader:
        print(x)


if __name__ == '__main__':
    dir_prefix = "/home/joan/IdeaProjects/mail-analytics/parse/enron_with_categories/"
    avro_schema = "/home/joan/IdeaProjects/mail-analytics/parse/mail/mail-schema.avsc"
    files = get_files(dir_prefix)
    emails = parse_mail(files)
    write_to_avro(avro_schema, emails)
    # read_from_avro()
