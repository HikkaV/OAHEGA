from elasticsearch import Elasticsearch


class ESLogger:
    def __init__(self, host, port, index='emotions',
                 settings=None):
        if settings:
            self.settings = settings
        else:
            self.settings = {
                "settings": {
                    "number_of_shards": 1,
                    "number_of_replicas": 0
                }
            }
        self.es = Elasticsearch([{'host': host, 'port': port}])
        self.index = index
        if self.es.ping():
            print('Connected to elastic')
        else:
            raise Exception
        self.check_index()

    def load_log(self, logs):
        res = self.es.index(index=self.index, doc_type='m', body=logs)
        if res:
            print('Successful')
        else:
            print("It's over Anakin!")

    def check_index(self):
        if not self.es.indices.exists(self.index):
            self.create_index()
        else:
            print('Index already exists')

    def recreate_index(self):
        self.es.indices.delete(index=self.index, ignore=[400, 404])
        self.create_index()

    def create_index(self):
        self.es.indices.create(index=self.index, ignore=400, body=self.settings)
        print('Created index')
