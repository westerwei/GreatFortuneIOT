'''
Created on 2014/12/19

@author: Wester
'''
import code
from pymongo.mongo_client import MongoClient
from bson.objectid import ObjectId

class Token(object):
    '''
    classdocs
    '''
    def __init__(self):
        self._access_token = None
        self._refresh_token = None
        self._token_type = None
        self._scopes = None
        self._expires = None
        self._client_id = None
        self._user_id = None
        self._user = None
    
    @property
    def access_token(self):
        return self._access_token
    
    @access_token.setter
    def access_token(self, access_token):
        self._access_token = access_token
        
    @property
    def refresh_token(self):
        return self._refresh_token
    
    @refresh_token.setter
    def refresh_token(self, refresh_token):
        self._refresh_token = refresh_token
        
    @property
    def token_type(self):
        return self._token_type
    
    @token_type.setter
    def token_type(self, token_type):
        self._token_type = token_type
        
    @property
    def scopes(self):
        return self._scopes
    
    @scopes.setter
    def scopes(self, scopes):
        self._scopes = scopes
        
    @property
    def expires(self):
        return self._expires
    
    @expires.setter
    def expires(self, expires):
        self._expires = expires
        
    @property
    def client_id(self):
        return self._client_id
    
    @client_id.setter
    def client_id(self, client_id):
        self._client_id = client_id
        
    @property
    def user_id(self):
        return self._user_id
    
    @user_id.setter
    def user_id(self, user_id):
        self._user_id = user_id
        
    @property
    def user(self):
        return self._user
    
    @user.setter
    def user(self, user):
        self._user = user
    
class Grant(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
        self._client_id = None
        self._code = None
        self._redirect_uri = None
        self._scopes = None
        self._user = None
        self._expires = None
        self._objid = None

    @property
    def client_id(self):
        return self._client_id
    
    @client_id.setter
    def client_id(self, client_id):
        self._client_id = client_id
        
    @property
    def code(self):
        return self._code
        
    @code.setter    
    def code(self, code):
        self._code = code
        
    @property    
    def redirect_uri(self):
        return self._redirect_uri
    
    @redirect_uri.setter
    def redirect_uri(self, redirect_uri):
        self._redirect_uri = redirect_uri
        
    @property
    def scopes(self):
        return self._scopes
    
    @scopes.setter
    def scopes(self, scopes):
        self._scopes = scopes
    
    @property
    def user(self):
        return self._user
    
    @user.setter
    def user(self, user):
        self._user = user
        
    @property
    def expires(self):
        return self._expires
    
    @expires.setter
    def expires(self, expires):
        self._expires = expires
    
    @property
    def objid(self):
        return self._objid
    
    @objid.setter
    def objid(self, objid):
        self._objid = objid
    
    def delete(self):
        
        client = MongoClient('localhost',27017)
        db = client.iotdb
        grantCol = db.Grant
        # make sure that every client has only one token connected to a user
        grantCol.remove({"_id" : ObjectId(self._objid)})
#         print 'Grant code will not be deleted.'
        return self
        
class User(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
        self._user_id = None
        self._userName = None
        
    @property
    def user_id(self):
        return self._user_id
    
    @user_id.setter
    def user_id(self, user_id):
        self._user_id = user_id
    
    @property
    def userName(self):
        return self._userName
    
    @userName.setter
    def userName(self, userName):
        self._userName = userName
    
class Client(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
        self._client_id = None
        self._client_secret = None
        self._client_type = None
        self._redirect_uris = None
        self._default_redirect_uri = None
        self._default_scopes = None 
        self._cust_id = None
        self._user_id = None
    
    @property
    def client_id(self):
        return self._client_id
    
    @client_id.setter
    def client_id(self, client_id):
        self._client_id = client_id
    
    @property
    def client_secret(self):
        return self._client_secret
    
    @client_secret.setter
    def client_secret(self, client_secret):
        self._client_secret = client_secret
        
    @property
    def client_type(self):
        return self._client_type
    
    @client_type.setter
    def client_type(self, client_type):
        self._client_type = client_type
        
    @property
    def redirect_uris(self):
        if self._redirect_uris:
            return self._redirect_uris
        return []
    
    @redirect_uris.setter
    def redirect_uris(self, redirect_uris):
        self._redirect_uris = redirect_uris
     
    @property
    def default_redirect_uri(self):
        return self._default_redirect_uri
    
    @default_redirect_uri.setter
    def default_redirect_uri(self, default_redirect_uri):
        self._default_redirect_uri = default_redirect_uri
    
    @property
    def default_scopes(self):
        return self._default_scopes
    
    @default_scopes.setter
    def default_scopes(self, default_scopes):
        self._default_scopes = default_scopes
    
    @property
    def cust_id(self):
        return self._cust_id
    
    @cust_id.setter
    def cust_id(self, cust_id):
        self._cust_id = cust_id
        
    @property
    def user_id(self):
        return self._user_id
    
    @user_id.setter
    def user_id(self, user_id):
        self._user_id = user_id
    
if __name__ == '__main__':
    pass