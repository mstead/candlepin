#! /usr/bin/env ruby

require 'active_support/all'
require 'awesome_print'
require 'gli'
require 'highline/import'
require 'json'
require 'openssl'
require 'uri'
require 'yaml'

require_relative './candlepin.rb'

module Candlepin
  class BadArgument < StandardError
    include GLI::StandardException
    attr_reader :exit_code

    def initialize(msg)
      super(msg)
      @exit_code = 2
    end
  end

  class UserProvided < Struct.new(:value)
  end

  class CPin
    include GLI::App

    DEFAULT_CONFIG_FILE = File.join(ENV['HOME'], '.cpinrc')

    attr_reader :client

    def initialize
      program_desc "A Candlepin command line client"
      version "1.0.0"
      subcommand_option_handling :normal
      arguments :strict

      # Tell HighLine to make an effort to wrap text
      $terminal.wrap_at = :auto

      define_error_handler
      define_acceptors

      create_global_options
      create_commands
      add_pre_hooks
    end

    def json_out(content)
      if @json_print_proc.nil?
        say(content.to_s)
      else
        @json_print_proc.call(content)
      end
    end

    def debug?
      @debug || ENV['CPIN_DEBUG'] || false
    end

    def verbose?
      @verbose || false
    end

    def warn(msg)
      say(HighLine.color("WARNING: #{msg}", :yellow))
    end

    def get_client(global_options)
      server = global_options[:server]
      cacert = global_options[:cacert]
      args = {
        :host => server.host,
        :port => server.port,
        :context => server.path,
        :insecure => @insecure,
        :ca_path => cacert
      }

      client = NoAuthClient.new(args)
      client.debug if debug?
      client
    end

    def add_pre_hooks
      pre do |global_options, command, options, args|
        # GLI puts options in as both strings and symbols.  That
        # is confusing, leads to duplication, and is unnecessary.  Even
        # worse if a option has a hyphen, GLI keeps the hyphen instead
        # of converting to an underscore leaving symbols like :"pretty-print"
        remove = []
        global_options.keys do |k|
          remove << k if k.kind_of?(Symbol)
        end
        # Drop all the symbols
        global_options.except!(*remove)

        global_options.deep_symbolize_keys!
        global_options.transform_keys! do |k|
          k.to_s.gsub('-', '_').to_sym
        end

        rc_file_options = read_rc_file(global_options[:config])
        # global_options.reverse_merge!(rc_file_options)
        require 'pry'; binding.pry

        @debug ||= global_options[:debug]

        # Turn on pretty printing (this will normally run)
        if global_options[:pretty_print]
          @json_print_proc = Proc.new do |content|
            # Negative indentation left-justifies the output
            formatted = content.awesome_inspect(:indent => -2, :index => false)
            say(formatted)
          end
        end

        # Turn off SSL/TLS verifications if connecting to localhost
        @insecure = global_options[:insecure]
        if global_options[:server].host == "localhost" &&
          global_options[:cacert].nil?
          warn("You are connecting to localhost and didn't give a CA cert.  Assuming you want --insecure.")
          @insecure = true
        end

        @client = get_client(global_options)
      end
    end

    def read_rc_file(rc_file)
      if rc_file != DEFAULT_CONFIG_FILE
        error_if_missing = true
      else
        error_if_missing = false
      end

      if File.exist?(rc_file)
        rc_settings = YAML.load(File.open(rc_file, 'r'))
        rc_settings.deep_symbolize_keys!
      elsif error_if_missing
        raise BadArgument.new("Cannot find #{rc_file}!")
      else
        rc_settings = {}
      end
      rc_settings
    end

    def define_error_handler
      on_error do |e|
        if e.kind_of?(GLI::StandardException)
          $stderr.puts(HighLine.color("ERROR: #{e.message}", :red))
          $stderr.puts(@debug)
          raise e if debug?
          # Must return false to avoid default on_error handler
          false
        else
          raise e
        end
      end
    end

    # I'm using these to perform some basic validations on arguments
    # passed in.  Note that these run *before* debug? is set by OptionParser
    # Use CPIN_DEBUG=1 as an environment variable if you need debug here.
    def define_acceptors
      accept(URI) do |val|
        begin
          server_url = URI.parse(val)
        rescue URI::InvalidURIError => e
          raise BadArgument.new(e.message)
        end
        unless server_url.kind_of?(URI::HTTP)
          raise BadArgument.new("#{server_url} is not an HTTP(S) URL")
        end
        server_url
      end

      accept(OpenSSL::X509::Certificate) do |val|
        unless File.exist?(val)
          raise BadArgument.new("The file '#{val}' does not exist.")
        end
        begin
          OpenSSL::X509::Certificate.new(File.read(val))
        rescue => e
          raise BadArgument.new("Could not read '#{val}': #{e.message}")
        end
        val
      end
    end

    def create_global_options
      switch(["verbose", "v"], :default_value => nil, :negatable => false)

      switch("debug", :default_value => nil, :negatable => false)

      switch("pretty-print", :default_value => true)

      switch(["insecure", "i"], :negatable => false)

      desc "Config file to read options from"
      flag(["config", "c"],
           :default_value => DEFAULT_CONFIG_FILE,
          )

      desc "Serve URL to connect to"
      flag("server",
           :default_value => URI('https://localhost:8443/candlepin'),
           :type => URI,
          )

      desc "CA that signed server certificate"
      flag("cacert",
           :type => OpenSSL::X509::Certificate,
          )
    end

    def create_commands
      desc "Check server status"
      command :status do |c|
        c.action do
          json_out(@client.get_status.content)
        end
      end
    end
  end
end

#     def read_rc_file(cmd)
#       if cmd.data.key?(:rc_file)
#         rc_file = cmd.data[:rc_file]
#         error_if_missing = true
#       else
#         rc_file = File.join(ENV['HOME'], '.cpinrc')
#         error_if_missing = false
#       end

#       if File.exist?(rc_file)
#         @rc_settings = YAML.load(File.open(rc_file, 'r'))
#         @rc_settings.deep_symbolize_keys!
#       elsif error_if_missing
#         error("Cannot find #{rc_file}!")
#         raise CmdParse::InvalidArgumentError.new
#       end
#     end
#   end
# end



    # @@rc_whitelist = [:debug, :verbose]

    # DEFAULT_RC = File.join(ENV['HOME'], '.cpinrc')
      # def read_rc_file(rc_file)
      #   if File.exist?(rc_file)
      #     rc_settings = YAML.load(File.open(rc_file, 'r'))
      #     rc_settings.deep_symbolize_keys!
      #   else
      #     say(rc_file)
      #     say_warning("Cannot find #{rc_file}!") unless rc_file == DEFAULT_RC
      #     rc_settings = {}
      #   end

      #   bad_settings = []
      #   rc_settings.each do |k, v|
      #     if @@rc_whitelist.include?(k)
      #       send(:"#{k}=", v)
      #     else
      #       bad_settings << k
      #     end
      #   end
      #   unless bad_settings.empty?
      #     say_error("The cpinrc file does not support these settings: #{bad_settings}")
      #   end
      # end
  # end
# end
